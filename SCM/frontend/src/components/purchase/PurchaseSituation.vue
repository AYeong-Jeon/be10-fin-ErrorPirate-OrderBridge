<script setup>
import {onMounted, ref, watch, computed} from 'vue';
import axios from "@/axios.js";
import dayjs from "dayjs";
import searchIcon from "@/assets/searchIcon.svg";

const searchStartDate = ref('');
const searchEndDate = ref('');
const searchName = ref('');
const purchaseSituationList = ref([]);
const purchaseSituationTotal = ref([]);

const fetchPurchaseSituationList = async () => {
  try {
    const params = {
      searchStartDate: searchStartDate.value,
      searchEndDate: searchEndDate.value,
      searchName: searchName.value,
    };

    const filteredParams = Object.fromEntries(
        Object.entries(params).filter(([_, value]) => value !== null && value !== undefined && value !== '')
    );

    const response = await axios.get(`purchase/situation`, {
      params: filteredParams,
      paramsSerializer: (params) => {
        return new URLSearchParams(params).toString();
      }
    });

    purchaseSituationTotal.value = response.data.pop();
    purchaseSituationList.value = response.data;
  } catch (error) {
    console.error("구매 현황 불러오기 실패 :", error);
  }
};

onMounted(() => {
  fetchPurchaseSituationList();
});

watch([searchStartDate, searchEndDate], () => {
  fetchPurchaseSituationList();
})

const printTable = () => {
  const printContent = document.getElementById('print-area').innerHTML; // 특정 영역 가져오기
  const originalContent = document.body.innerHTML; // 현재 페이지 내용 저장

  // 페이지 내용을 특정 영역으로 교체
  document.body.innerHTML = printContent;

  // 인쇄
  window.print();

  // 원래 내용 복원
  document.body.innerHTML = originalContent;

  // SPA일 경우 Vue의 리렌더링 강제 호출
  location.reload(); // 상태를 새로고침하여 업데이트
}

const excelDown = async () => {
  const excelName = "구매현황_" + new Date().getFullYear() + (new Date().getMonth() + 1) + new Date().getDay();
  try {
    const params = {
      searchStartDate: searchStartDate.value,
      searchEndDate: searchEndDate.value,
      searchName: searchName.value,
    };

    const filteredParams = Object.fromEntries(
        Object.entries(params).filter(([_, value]) => value !== null && value !== undefined && value !== '')
    );

    const response = await axios.get(`purchase/situation/excelDown`, {
      params: filteredParams,
      paramsSerializer: (params) => {
        return new URLSearchParams(params).toString();
      },
      responseType: "blob", // 중요: blob 형식으로 설정
    });

    // Blob 객체 생성 및 다운로드 처리
    const blob = new Blob([response.data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = decodeURIComponent(excelName); // 파일명 디코딩
    link.click();

    // Blob URL 해제
    URL.revokeObjectURL(link.href);

  } catch (error) {
    console.error("구매현황 엑셀다운로드 실패 :", error);
  }
}

</script>

<template>
  <div class="row">
    <div class="col-md-3">
      <div class="side-box card">
        <div class="card-body">
          <p class="card-title">구매일자</p>
          <input type="date" v-model="searchStartDate"/> ~ <input type="date" v-model="searchEndDate"/>
        </div>
      </div>
      <div class="side-box card">
        <div class="card-body">
          <p class="card-title">구매서명</p>
          <b-input-group class="mt-3">
            <b-form-input v-model="searchName"></b-form-input>
            <b-button variant="light" class="button" @click="fetchPurchaseOrderSituationList()">
              <searchIcon class="icon"/>
            </b-button>
          </b-input-group>
        </div>
      </div>
    </div>
    <div class="col-md-9">
      <div class="d-flex justify-content-end mt-3">
        <b-button @click="excelDown()" variant="light" size="sm" class="button ms-2 mb-3">엑셀 다운로드</b-button>
        <b-button @click="printTable()" variant="light" size="sm" class="button ms-2 mb-3">인쇄</b-button>
      </div>
      <div id="print-area" class="content">
        <div class="table-container">
          <!-- 테이블 -->
          <table>
            <thead>
            <tr>
              <th>번호</th>
              <th>구매일자</th>
              <th>구매서명</th>
              <th>총 수량</th>
              <th>금액</th>
              <th>구매 계약일</th>
              <th>구매서 비고</th>
            </tr>
            </thead>
            <tbody v-if="purchaseSituationList.length > 0">
            <!-- 필터링된 결과 및 월별 합계 출력 -->
            <template v-for="(purchaseSituation, index) in purchaseSituationList" :key="index">
              <tr v-if="purchaseSituation.purchaseRegDate">
                <td>{{ index + 1 }}</td>
                <td>{{ dayjs(purchaseSituation.purchaseRegDate).format('YYYY/MM/DD HH:mm:ss') }}</td>
                <td>{{ purchaseSituation.purchaseName }}</td>
                <td>{{ purchaseSituation.purchaseTotalQuantity !== null ? purchaseSituation.purchaseTotalQuantity.toLocaleString() : '0' }}</td>
                <td> ￦ {{ purchaseSituation.purchaseExtendedPrice !== null ? purchaseSituation.purchaseExtendedPrice.toLocaleString() : '0' }}</td>
                <td>{{ dayjs(purchaseSituation.purchaseContractDate).format('YYYY/MM/DD HH:mm:ss') }}</td>
                <td>{{ purchaseSituation.purchaseNote !== null ? purchaseSituation.purchaseNote : '-' }}</td>
              </tr>
              <tr v-else class="monthly-total">
                <td> -</td>
                <td>{{ purchaseSituation.purchaseRegMonth }}</td>
                <td> -</td>
                <td>{{ purchaseSituation.purchaseMonthQuantity.toLocaleString() }}</td>
                <td> ￦ {{ purchaseSituation.purchaseMonthPrice.toLocaleString() }}</td>
                <td> -</td>
                <td> -</td>
              </tr>
            </template>

            </tbody>
            <tbody v-else>
            <tr>
              <td colspan="6">해당 검색조건에 부합한 구매서 존재하지 않습니다</td>
            </tr>
            </tbody>
            <!-- 총합 -->
            <tfoot v-if="purchaseSituationTotal">
            <tr>
              <td colspan="5">총합</td>
              <td colspan="2">￦ {{ purchaseSituationTotal.purchaseMonthPrice }}</td>
            </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.button {
  background-color: #FFF8E7;
  border: 1px solid;
}

.content {
  display: flex;
  justify-content: space-around;
  max-height: 400px; /* 스크롤바가 나타날 최대 높이 */
  overflow-y: auto; /* 수직 스크롤바 */
}


.table-container {
  width: 100%;
  overflow-x: auto; /* 가로 스크롤바 */
}

.search-input > input {
  width: 100%;
  border: 1px solid #D9D9D9;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
}

.search-input > img {
  position: absolute;
  width: 17px;
  top: 10px;
  right: 12px;
  margin: 0;
}

.table-container {
  border: 1px solid #000000;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: center;
}

th, td {
  padding: 10px;
  border: none;
}

thead th {
  border-bottom: 4px solid #AAAAAA; /* 헤더 아래쪽만 선 표시 */
}

tfoot tr {
  border-top: 4px solid #AAAAAA;
}

tfoot {
  font-weight: bold;
}

.monthly-total {
  font-weight: bold;
  border-top: 2px solid #AAAAAA;
  border-bottom: 1px solid #AAAAAA;
}

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

.icon {
  width: 20px;
  height: 20px;
}

/* 인쇄 스타일 */
@media print {
  /* 인쇄할 영역만 표시 */
  body * {
    visibility: hidden; /* 전체 요소 숨기기 */
  }

  #print-area, #print-area * {
    visibility: visible; /* 특정 영역만 표시 */
  }

  #print-area {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
  }

  /* 버튼 숨기기 */
  button {
    display: none;
  }
}
</style>