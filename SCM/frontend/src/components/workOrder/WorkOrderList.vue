<script setup>
import axios from "@/axios"
import {onMounted, ref, watch} from "vue";
import { sSuccess, sError, sServerError, sWarning, sConfirm } from '@/common/salert';
import searchIcon from "@/assets/searchIcon.svg";
import dayjs from "dayjs";
import printIcon from "@/assets/printIcon.svg"
import editIcon from "@/assets/editIcon.svg"
import trashIcon from "@/assets/trashIcon.svg"
import router from "@/router/index.js";
import WorkOrderPrintPreview from "@/components/workOrder/WorkOrderPrintPreview.vue";

const workOrderList = ref([]);
const workOrderStatusList = ref([]);
const workOrderDetail = ref([]);

const searchStartDate = ref(null);
const searchEndDate = ref(null);
const searchFactory = ref(null);
const searchStatus = ref(new Set([]));
const itemDivisionList = ref([]);

const searchPage = ref(1);
const searchSize = ref(10);
const currentPage = ref(1);
const totalPage = ref(1);
const totalCount = ref(0);

const isModalVisible = ref(false);
const selectedWorkOrder = ref(null);
const fullDetail = ref({});

const openPrintPreview = (workOrder) => {
  if (!workOrder) {
    console.error('선택된 작업지시서가 없습니다.');
    return;
  }
  console.log('openPrintPreview 호출됨, 선택된 작업지시서:', workOrder);
  selectedWorkOrder.value = workOrder;
  isModalVisible.value = true;
};

const closePrintPreview = () => {
  isModalVisible.value = false;
  selectedWorkOrder.value = null;
};

// 목록조회
const fetchWorkOrderList = async () => {
  workOrderDetail.value = [];
  try {
    const response = await axios.get(`workOrder`, {
      params: {
        page: searchPage.value,
        size: searchSize.value,
        startDate: searchStartDate.value,
        endDate: searchEndDate.value,
        warehouseName: searchFactory.value,
        workOrderStatus: searchStatus.value.size === 0 ? null : Array.from(searchStatus.value),
      }, paramsSerializer: (params) => {
        // null이나 undefined 값을 필터링
        const filteredParams = Object.fromEntries(
            Object.entries(params).filter(([_, value]) => value !== null && value !== undefined)
        );
        return new URLSearchParams(filteredParams).toString();
      }
    });
    console.log(response.data);
    workOrderList.value = response.data.workOrderList;
    workOrderStatusList.value = response.data.workOrderStatusList;

    totalCount.value = response.data.totalItems;
    currentPage.value = response.data.currentPage;
    totalPage.value = response.data.totalPages;
    console.log(workOrderList.value);
  } catch (error) {
    if (error.response.data.errorCode === 'COMMON_ERROR_002') {
      await sServerError(error);
    }
    console.log("작업지시서 불러오기 실패: ", error);
  }
};

// 상세보기
const fetchWorkOrderDetail = async (workOrderSeq) => {
  if(workOrderDetail.value[workOrderSeq] === undefined) {
    try {
      const response = await axios.get(`workOrder/${workOrderSeq}`, {});
      console.log(response.data);

      workOrderDetail.value[workOrderSeq] = {
        workOrderSeq : response.data.workOrderDetail.workOrderSeq,
        workOrderName : response.data.workOrderDetail.workOrderName,
        workOrderIndicatedDate : response.data.workOrderDetail.workOrderIndicatedDate,
        workOrderStatus : response.data.workOrderDetail.workOrderStatus,
        workOrderPrice : response.data.workOrderDetail.workOrderPrice,
        workOrderIndicatedQuantity : response.data.workOrderDetail.workOrderIndicatedQuantity,
        workOrderWorkQuantity: response.data.workOrderDetail.workOrderWorkQuantity ?? 0,
        userName : response.data.workOrderDetail.userName,
        userPhoneNo : response.data.workOrderDetail.userPhoneNo,
        clientName : response.data.workOrderDetail.clientName,
        workOrderEndDate : response.data.workOrderDetail.workOrderEndDate,
        workOrderDueDate : response.data.workOrderDetail.workOrderDueDate,
        warehouseName : response.data.workOrderDetail.warehouseName,
        workOrderNote : response.data.workOrderDetail.workOrderNote,
        workOrderItem: Array.isArray(response.data.workOrderItem)
            ? response.data.workOrderItem
            : [response.data.workOrderItem || {}],
      }

      fullDetail.value[workOrderSeq] = response.data;
      console.log('fullDetail',fullDetail.value[workOrderSeq]);

      console.log(response.data.workOrderDetail);
      console.log(response.data.workOrderItem);
    } catch (error) {
      if (error.response.data.errorCode === 'WORK_ORDER_ERROR_001') {
        await sServerError(error);
      }

      console.error("작업지시서 상세 불러오기 실패 :", error);
    }
  } else {
    workOrderDetail.value[workOrderSeq] = undefined;
  }
};

// 품목 분류 요청
const fetchItemDivision = async () => {
  try {
    const response = await axios.get(`item/item-division`);

    itemDivisionList.value = response.data;
  } catch (error) {
    console.log(`품목 분류 요청 실패 ${error}`);
  }
}

// 상태 키로 값 반환
function findStatusValue(array, key) {
  for (const item of array) {
    if (item.key === key) {
      return item.value
    }
  }
}

// 작업지시서 목록 엑셀 다운로드
const excelDown = async () => {
  try {
    const response = await axios.get(`workOrder/excelDownload`, {
      params: {
        startDate: searchStartDate.value,
        endDate: searchEndDate.value,
        warehouseName: searchFactory.value,
        workOrderStatus: searchStatus.value.size === 0 ? null : Array.from(searchStatus.value),
      }, responseType: "blob"
    });
    const blob = new Blob([response.data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;

    // 헤더 인식이 안될때 대비
    let fileName = '작업지시서 download.xlsx';
    const disposition = response.headers['content-disposition'];

    if (disposition) {
      const fileNameMatch = disposition.match(/filename="?([^"]+)"?/);
      if (fileNameMatch) {
        fileName = decodeURIComponent(fileNameMatch[1]);
      }
    }

    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  } catch (error) {
        console.error('Excel 다운로드 실패:', error);
    if (error.response.data.errorCode === 'EXCEL_DOWN_ERROR_001') {
      await sServerError(error);
    }
  }
};

// 삭제
const deleteWorkOrder = async (workOrderSeq) => {
  const result = await sConfirm("이 작업지시서를 삭제하시겠습니까?");
  console.log("삭제요청 작업지시서 번호", workOrderSeq);
  if (result) {
    try {
      const response = await axios.delete(`workOrder/${workOrderSeq}`);
      await sSuccess('삭제가 완료되었습니다.');

      search(); // 삭제 후 목록 갱신
    } catch (error) {
      console.error("작업지시서 삭제 요청 실패:", error);
      if (error.response.data.errorCode === 'WORK_ORDER_ERROR_001') {
        await sServerError(error);
      } else if (error.response.data.errorCode === 'WORK_ORDER_ERROR_005') {
        await sWarning('결재 전이거나 중단된 작업지시서만 삭제 가능합니다.');
      } else if (error.response.data.errorCode === 'SECURITY_ERROR_001') {
        await sWarning('작성자만 삭제 가능합니다.');
      }  else {
        await sError('삭제에 실패했습니다. 다시 시도해주세요.');
      }
    }
  }
}

// 등록 페이지로 이동
const goToWriteWorkOrder = () => {
  router.push(`/work-order/write`);
}

// 수정 페이지로 이동
const goToEdit = (workOrderSeq) => {
  router.push(`/work-order/edit/${workOrderSeq}`);
}

// 숫자 포맷
function numberFormating(number) {
  return `${number.toString().replace(/\B(?<!\.\d*)(?=(\d{3})+(?!\d))/g, ",")}`;
}

onMounted(() => {
  fetchWorkOrderList();
  fetchItemDivision();

  const startOfMonth = dayjs().startOf('month').format('YYYY-MM-DD'); // 해당 달의 첫 날
  // const today = dayjs().format('YYYY-MM-DD'); // 오늘 날짜 (YYYY-MM-DD 형식)
  const endOfMonth = dayjs().endOf('month').format('YYYY-MM-DD')

  searchStartDate.value = startOfMonth;
  searchEndDate.value = endOfMonth;
});

watch([searchStartDate, searchEndDate], () => {
  search();
});

watch(searchPage, () => {
  fetchWorkOrderList();
});

function check(status) {
  if(searchStatus.value.has(status)) {
    searchStatus.value.delete(status);
  } else {
    searchStatus.value.add(status);
  }

  search();
}

function search() {
  searchPage.value = 1;
  fetchWorkOrderList();
}

</script>

<template>
  <div class="row">
    <div class="col-md-3">
      <div class="side-box card">
        <div class="card-body">
          <p class="card-title">작업지시일</p>
          <input type="date"  v-model="searchStartDate"/> ~
          <input type="date"  v-model="searchEndDate"/>
        </div>
      </div>
      <div class="side-box card">
        <div class="card-body">
          <p class="card-title">생산공장명</p>
          <b-input-group class="mt-3">
            <b-form-input v-model="searchFactory" @keyup.enter="search()"></b-form-input>
            <b-button variant="light" class="button" @click="search()"><searchIcon class="icon"/></b-button>
          </b-input-group>
        </div>
      </div>
      <div class="side-box card">
        <div class="card-body">
          <p class="card-title">작업지시서 상태</p>
          <template v-for="workOrderStatus in workOrderStatusList">
            <b-form-checkbox v-if="workOrderStatus.key !== 'DELETE'"
                             @click="check(workOrderStatus.key)">{{workOrderStatus.value}}</b-form-checkbox>
          </template>
        </div>
      </div>
    </div>
    <div class="col-md-9">
      <div>
        <div class="d-flex justify-content-between">
          <div>검색결과: {{ totalCount }}개</div>
          <div class="d-flex justify-content-end mt-3">
            <b-button @click="excelDown" variant="light" size="sm" class="button">엑셀 다운로드</b-button>
            <b-button @click="goToWriteWorkOrder" variant="light" size="sm" class="button ms-2">작업지시서 등록</b-button>
          </div>
        </div>
        <div class="list-headline row">
          <div class="list-head col-5">작업지시서명</div>
          <div class="list-head col-2">생산공장명</div>
          <div class="list-head col-3">작업지시일</div>
          <div class="list-head col-2">상태</div>
        </div>
        <template v-if="workOrderList.length > 0">
          <div style="max-height: 600px; overflow-y: auto;">
            <div v-for="workOrder in workOrderList"
                 :key="workOrder.workOrderSeq"
                 @click="fetchWorkOrderDetail(workOrder.workOrderSeq)">
              <div class="list-line row" :id="'print-area-' + workOrder.workOrderSeq">
                <div class="list-body col-5 left">
                  <b>{{ workOrder.workOrderName }}</b>
                  <div v-if="!workOrder.itemName"><br></div>
                  <div v-else>{{ workOrder.itemName }}</div>
                </div>
                <div class="list-body col-2">{{ workOrder.warehouseName }}</div>
                <div class="list-body col-3">{{ dayjs(workOrder.workOrderIndicatedDate).format('YYYY/MM/DD') }}</div>
                <div class="list-body col-2">{{ findStatusValue(workOrderStatusList, workOrder.workOrderStatus) }}</div>

                <!-- 확장된 상세 정보 표시 -->
                <div class="d-flex justify-content-center" v-if="workOrderDetail[workOrder.workOrderSeq]">
                  <div class="col-md-11 mt-3">
                    <b>총 주문액 : </b> {{ numberFormating(workOrderDetail[workOrder.workOrderSeq].workOrderPrice) }} ₩<br>
                    <b>총 주문량 : </b>{{ numberFormating(workOrderDetail[workOrder.workOrderSeq].workOrderIndicatedQuantity) }} <br>
                    <b>작업완료 수량 : </b>{{ numberFormating(workOrderDetail[workOrder.workOrderSeq].workOrderWorkQuantity) }} <br>
                    <b>담당자 : </b>{{ workOrderDetail[workOrder.workOrderSeq].userName }} <br>
                    <b>납품처명 : </b>{{ workOrderDetail[workOrder.workOrderSeq].clientName}} <br>
                    <b>작업목표일 : </b>{{ dayjs(workOrderDetail[workOrder.workOrderSeq].workOrderDueDate).format('YYYY-MM-DD HH:mm:ss') }}<br>
<!--                    <b>작업완료일 : </b>-->
                    <div v-if ="workOrderDetail[workOrder.workOrderSeq].workOrderEndDate != null"> <b>작업완료일 : </b>{{ dayjs(workOrderDetail[workOrder.workOrderSeq].workOrderEndDate).format('YYYY-MM-DD HH:mm:ss') }}</div>
                    <div v-else><b>작업완료일 : </b> -</div>
                    <b>작업지시서 비고 : </b>{{ workOrderDetail[workOrder.workOrderSeq].workOrderNote }}<br>

                    <!--  품목정보  -->
                    <div style="display:flex; flex-wrap: wrap;">
                      <template v-for="workOrderItem in workOrderDetail[workOrder.workOrderSeq].workOrderItem" :key="workOrderItem.itemSeq">
                        <div class="card item-card">
                          <img :src=workOrderItem.itemImageUrl class="card-img-top">
                          <div style="margin: 5px;">
                            <small>{{ findStatusValue(itemDivisionList, workOrderItem.itemDivision) }}</small>
                            <div style="display: flex; justify-content: space-between;">
                              <b style="font-size: medium;">{{ workOrderItem.itemName }}</b>
                            </div>
                            <small>{{ numberFormating( workOrderDetail[workOrder.workOrderSeq].workOrderIndicatedQuantity) }}개
                                    / 개당 {{ numberFormating(workOrderItem.itemPrice) }} 원</small><br>
                            <small>{{ workOrderItem.ingredientWarehouseName }}</small>
                            <br><br>
                            <small v-if="workOrderItem.itemNote" style="margin-top: 20px;">
                              비고: {{ workOrderItem.itemNote }}</small>
                            <small v-else style="margin-top: 20px;">비고: 없음</small>
                          </div>
                        </div>
                      </template>
                    </div>
                    <div class="d-flex justify-content-end align-items-center">
<!--                      <printIcon class="me-3 icon" v-if="workOrderDetail[workOrder.workOrderSeq]"-->
<!--                                 @click.stop="openPrintPreview(workOrderDetail[workOrder.workOrderSeq])"/>-->
                      <printIcon class="me-3 icon"  v-if="fullDetail[workOrder.workOrderSeq]"
                                 @click.stop="openPrintPreview(fullDetail[workOrder.workOrderSeq])"/>
                      <editIcon class="me-3 icon" @click.stop="goToEdit(workOrder.workOrderSeq)"/>
                      <trashIcon class="icon" @click.stop="deleteWorkOrder(workOrder.workOrderSeq)"/>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="pagination">
            <b-pagination
                v-model="searchPage"
                :totalRows="totalCount"
                :perPage="searchSize">
            </b-pagination>
          </div>
        </template>
        <template v-else>
          <b-card-text class="no-list-text">해당 검색조건에 부합한 작업지시서가 존재하지 않습니다.</b-card-text>
        </template>
      </div>

    </div>
  </div>

  <WorkOrderPrintPreview
      :isVisible="isModalVisible"
      :workOrder="selectedWorkOrder"
      :isList=true
      @close="closePrintPreview"
  />
</template>

<style scoped>

div {
  font-size: small;
}

.button {
  background-color: #FFF8E7;
  border: 1px solid;
}

.side-box {
  min-height: 100px;
  margin-top: 20px;
}

.card-title {
  font-size: medium;
  font-weight: bold;
}

.list-headline {
  border-bottom: 1px solid black;
  margin-bottom: 10px;
  padding: 20px 40px 20px 20px;
}

.list-head {
  text-align: center;
}

.list-line {
  width: 99%;
  border: 1px solid Silver;
  border-radius: 8px;
  margin-left:1px;
  margin-top: 20px;
  padding: 10px 5px 10px 5px;
}

.list-body {
  text-align: center;
  margin: auto 0;
}

.pagination {
  justify-content: center; /* 가로 중앙 정렬 */
  margin-top: 20px;
}

.left {
  text-align: left;
}

.no-list-text {
  text-align: center;
  margin-top: 10%;
}

.icon {
  width: 20px;
  height: 20px;
}

.item-card {
  width: 220px;
  margin: 10px;
}

.item-card img {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.card-img-top {
  max-height: 90px;
  object-fit: cover;
}

</style>
