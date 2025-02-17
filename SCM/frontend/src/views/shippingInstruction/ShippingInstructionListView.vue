<script setup>
import ShippingInstructionList from "@/components/shippingInstruction/ShippingInstructionList.vue";
import {onMounted, reactive, ref, watch} from "vue";
import { sSuccess, sServerError, sWarning } from '@/common/salert';
import axios from "@/axios"
import router from "@/router/index.js";

const totalCount = ref(0);
const pageSize = ref(10);
const pageNumber = ref(1);
const shippingInstructionList = ref([]);
const shippingInstructionStatusList = ref([]);
const shippingAddressList = ref([]);

const searchStartDate = ref(null);
const searchEndDate = ref(null);
const searchName = ref(null);
const searchStatus = ref(new Set([]));
const expandData = ref({});
const itemDivisionList = ref([]);

// 출하지시서 목록 요청
const fetchShippingInstructionList = async () => {
  try {
    const response = await axios.get(`shipping-instruction`, {
      params: {
        startDate: searchStartDate.value,
        endDate: searchEndDate.value,
        clientName: searchName.value,
        shippingInstructionStatus: searchStatus.value.size === 0 ? null : Array.from(searchStatus.value),
        page: pageNumber.value,
        size: pageSize.value
      }, paramsSerializer: (params) => {
        // null이나 undefined 값을 필터링
        const filteredParams = Object.fromEntries(
            Object.entries(params).filter(([_, value]) => value !== null && value !== undefined)
        );
        return new URLSearchParams(filteredParams).toString();
      }
    });

    shippingInstructionList.value = response.data.shippingInstructionList;
    totalCount.value = response.data.totalItems;

  } catch (error) {
    console.error("출하지시서 목록 불러오기 실패 :", error);
  }
};

// 상세 출하지시서 요청
const fetchShippingInstruction = async (seq) => {
  try {
    const response = await axios.get(`shipping-instruction/${seq}`, {});

    expandData.value[seq] = response.data; // ref 값에 추가

  } catch (error) {
    console.error("상세 출하지시서 불러오기 실패 :", error);
  }
};

// 출하지시서 삭제 요청
const deleteShippingInstruction = async (seq) => {

  try {
    const response = await axios.delete(`shipping-instruction/${seq}`, {});

    await sSuccess("출하지시서가 삭제되었습니다.");

  } catch (error) {
    if (error.response) {
      // 서버에서 반환된 상태 코드에 따른 처리
      if (error.response.status === 400) {
        console.error("상세 출하지시서 삭제 실패 : 결재후");
        await sWarning("이미 결재 후이므로 삭제하실 수 없습니다.");
      } else {
        console.error(`상세 출하지시서 삭제 실패 : 상태 코드 ${error.response.status}`);
      }
    }
  }
};

// 출하지시서 상태 목록 요청
const fetchShippingInstructionStatusList = async () => {
  try {
    const response = await axios.get(`shipping-instruction/status`, {});

    shippingInstructionStatusList.value = response.data;

  } catch (error) {
    console.error("출하지시서 상태 목록 불러오기 실패 :", error);
  }
};

// 출하주소 목록 요청
const fetchShippingAddressList = async () => {
  try {
    const response = await axios.get(`shipping-instruction/address`, {});

    shippingAddressList.value = response.data;

  } catch (error) {
    console.error("출하주소 목록 불러오기 실패 :", error);
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

// 엑셀 다운 요청
const excelDown = async () => {
  try {
    const response = await axios.get(`shipping-instruction/excel`, {
      params: {
        startDate: searchStartDate.value,
        endDate: searchEndDate.value,
        clientName: searchName.value,
        shippingInstructionStatus: searchStatus.value.size === 0 ? null : Array.from(searchStatus.value),
      }, paramsSerializer: (params) => {
        // null이나 undefined 값을 필터링
        const filteredParams = Object.fromEntries(
            Object.entries(params).filter(([_, value]) => value !== null && value !== undefined)
        );
        return new URLSearchParams(filteredParams).toString();
      },
      responseType: "blob", // 중요: blob 형식으로 설정
    });

    const blob = new Blob([response.data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    });

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = decodeURIComponent(response.headers["content-disposition"].split('filename=')[1]);
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);

  } catch (error) {
    console.error("출하지시서 엑셀다운로드 실패 :", error);
  }
}

// 출하전표 등록 요청
const createShippingSlip = async (seq) => {
  try {
    const response = await axios.post('shipping-slip',
        {
          shippingSlipShippingDate: expandData.value[seq].shippingInstructionDTO.shippingInstructionScheduledShipmentDate,
          shippingInstructionSeq: expandData.value[seq].shippingInstructionDTO.shippingInstructionSeq,
          shippingSlipNote: expandData.value[seq].shippingInstructionDTO.shippingInstructionNote,
          shippingSlipItems: expandData.value[seq].itemList.map(item => ({
            itemSeq: item.itemSeq,
            shippingSlipItemQuantity: item.shippingInstructionItemQuantity,
            shippingSlipItemNote: item.shippingInstructionItemNote,
          })),
        }, {});

    await sSuccess('출하전표가 등록되었습니다!');
    // 조회 페이지 이동
    await router.push("/shipping-instruction");

  } catch (error) {
    if (error.response) {
      // 서버에서 반환된 상태 코드에 따른 처리
      if (error.response.status === 400) {
        console.error(`출하전표 등록 실패 : ${error.response.data.message}`);
        await sServerError(error);
      } else {
        console.error(`출하전표 등록 실패 : 상태 코드 ${error.response.status}`);
      }
    }
  }
};

// 거래처 힌트 요청
const clientHintList = ref(null);
let clientSearchCount = 0;

const fetchClientHint = async (clientName) => {
  if (clientName.value === "") {
    clientHintList.value = null;
  } else {
    try {
      const response = await axios.get(`client/hint`, {
        params: {
          keyword: clientName.value
        }
      });
      if (response.data.length > 0) {
        clientHintList.value = response.data;
        clientSearchCount = 0;
      } else if (clientSearchCount > 2) {
        clientHintList.value = null;
      } else { clientSearchCount++; }
    } catch (error) {
      console.log(`거래처 힌트 요청 실패 ${error}`)
    }
  }
  if (clientHintList.value) {
    if (clientHintList.value.length === 1 && clientHintList.value[0] === searchClient.value) {
      clientHintList.value = null;
    }
  }
}

onMounted(async () => {
  await fetchShippingInstructionList();

  await fetchShippingInstructionStatusList();
  await fetchShippingAddressList();
  await fetchItemDivision();
});

// 페이지 이동
const handlePage = (newPageNumber) => {
  pageNumber.value = Number(newPageNumber.value);
  fetchShippingInstructionList();
};

// 거래처 추천
const handleClient = (newClient) => {
  fetchClientHint(newClient);
}

// 검색
const handleSearch = (payload) => {
  searchStartDate.value = payload.startDate;
  searchEndDate.value = payload.endDate;
  searchName.value = payload.clientName;

  search();
};

// 상태 검색
const handleStatus = (payload) => {
  if (searchStatus.value.has(payload)) {
    searchStatus.value.delete(payload);
  } else {
    searchStatus.value.add(payload);
  }

  search();
};

// 수정 페이지로 이동
const handleEdit = (seq) => {
  if (seq != null) {
    router.push(`/shipping-instruction/edit/${seq}`);
  }
}

// 삭제 수행
const handleDelete = async (seq) => {
  if (seq != null) {
    await deleteShippingInstruction(seq);
  }
  await fetchShippingInstructionList();
};

// 출하전표 등록 수행
const handleShippingSlip = (seq) => {
  createShippingSlip(seq);
}

function search() {
  pageNumber.value = 1;

  fetchShippingInstructionList();
}

// 상세 정보 확장
const handleExtendItem = (seq) => {
  if (expandData.value[seq]) {
    // 이미 확장된 상태면 축소
    delete expandData.value[seq];
  } else {
    // API로 데이터를 가져와서 저장
    fetchShippingInstruction(seq);
  }
}
</script>

<template>
  <h4 class="title">영업관리 > 출하지시서 조회</h4>
  <ShippingInstructionList :searchStartDate="searchStartDate"
                           :searchEndDate="searchEndDate"
                           :searchName="searchName"
                           :shippingInstructionList="shippingInstructionList"
                           :shippingInstructionStatusList="shippingInstructionStatusList"
                           :shippingAddressList="shippingAddressList"
                           :totalCount="totalCount"
                           :pageNumber="pageNumber"
                           :pageSize="pageSize"
                           :expandData="expandData"
                           :itemDivisionList="itemDivisionList"
                           :clientHintList="clientHintList"
                           @pageEvent="handlePage"
                           @clientEvent="handleClient"
                           @searchEvent="handleSearch"
                           @checkStatusEvent="handleStatus"
                           @extendItemEvent="handleExtendItem"
                           @registerEvent="router.push('/shipping-instruction/input')"
                           @itemEditEvent="handleEdit"
                           @itemDeleteEvent="handleDelete"
                           @excelEvent="excelDown"
                           @shippingSlipRegisterEvent="handleShippingSlip"
  />
</template>

<style scoped>

.title {
  padding-bottom: 20px;
}

</style>
