<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createOrder } from '@/api/order'
import { createCustomer, searchCustomers } from '@/api/customer'
import { checkResource, type CheckResult } from '@/api/resource'
import type { Customer } from '@/api/customer'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSMapPicker from '@/components/BBPMSMapPicker.vue'

const router = useRouter()

const formRef = ref()
const submitting = ref(false)
const checking = ref(false)
/** 资源核查结果（下单回写） */
const checkResult = ref<CheckResult | null>(null)

async function onCheckResource() {
  if (!form.address.trim()) {
    ElMessage.warning('请先输入安装地址')
    return
  }
  checking.value = true
  try {
    checkResult.value = await checkResource(form.address)
    const st = checkResult.value.status
    if (st === 'RESOURCE_OK') ElMessage.success(checkResult.value.message)
    else ElMessage.warning(checkResult.value.message)
  } catch (e: any) {
    ElMessage.error(e?.msg || e?.message || '核查失败')
  } finally {
    checking.value = false
  }
}

const form = reactive({
  customerMode: 'EXISTING', // EXISTING | NEW
  customerId: '',
  customerName: '',
  customerPhone: '',
  customerIdCard: '',
  packageId: '',
  address: '',
  location: undefined as { lng: number; lat: number; address?: string } | undefined,
  appointmentAt: '' as string,
  remark: ''
})

const customerOptions = ref<Customer[]>([])

async function searchCustomer(q: string) {
  if (!q) { customerOptions.value = []; return }
  const list = await searchCustomers(q, 10)
  customerOptions.value = list
}

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  customerName: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
  customerPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  packageId: [{ required: true, message: '请选择套餐', trigger: 'change' }],
  address: [{ required: true, message: '请输入安装地址', trigger: 'blur' }],
  appointmentAt: [{ required: true, message: '请选择预约时间', trigger: 'change' }]
}

const packageOptions = [
  { value: 'PKG_100M', name: '100M Broadband', label: '100M Broadband - ¥99/month' },
  { value: 'PKG_300M', name: '300M Broadband', label: '300M Broadband - ¥159/month' },
  { value: 'PKG_500M', name: '500M Broadband', label: '500M Broadband - ¥199/month' },
  { value: 'PKG_1G', name: '1G Broadband', label: '1G Broadband - ¥299/month' }
]

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      let customerId: number | string = form.customerId
      if (form.customerMode === 'NEW') {
        customerId = await createCustomer({
          name: form.customerName,
          phone: form.customerPhone,
          idCardNo: form.customerIdCard,
          address: form.address
        })
      }
      const selectedPackage = packageOptions.find((p) => p.value === form.packageId)
      const payload = {
        customerId,
        packageCode: form.packageId,
        packageName: selectedPackage?.name || form.packageId,
        installAddress: form.address,
        expectedInstallDate: form.appointmentAt,
        appointmentTime: form.appointmentAt,
        contactPhone: form.customerMode === 'NEW' ? form.customerPhone : undefined,
        remark: form.remark,
        // 资源核查回写（ITERATION 2）
        roomId: checkResult.value?.roomId,
        resourceStatus: checkResult.value?.status,
        checkRemark: checkResult.value?.message
      }
      const createdId = await createOrder(payload)
      ElMessage.success('Order created')
      router.push(`/order/detail/${createdId}`)
    } finally {
      submitting.value = false
    }
  })
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="创建订单" description="提交一条新的宽带报装订单" />

    <div class="app-card" style="max-width: 880px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="客户">
          <el-radio-group v-model="form.customerMode">
            <el-radio-button value="EXISTING">已有客户</el-radio-button>
            <el-radio-button value="NEW">新客户</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.customerMode === 'EXISTING'">
          <el-form-item label="Search" prop="customerId">
            <el-select
              v-model="form.customerId"
              filterable
              remote
              :remote-method="searchCustomer"
              :loading="false"
              placeholder="按姓名或手机号搜索"
              style="width: 100%"
            >
              <el-option
                v-for="c in customerOptions"
                :key="c.id"
                :label="`${c.name} (${c.phone})`"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="名称" prop="customerName">
            <el-input v-model="form.customerName" />
          </el-form-item>
          <el-form-item label="手机号" prop="customerPhone">
            <el-input v-model="form.customerPhone" maxlength="11" />
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="form.customerIdCard" maxlength="18" />
          </el-form-item>
        </template>

        <el-form-item label="套餐" prop="packageId">
          <el-select v-model="form.packageId" placeholder="选择套餐" style="width: 100%">
            <el-option v-for="p in packageOptions" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" />
          <div style="margin-top: 6px; display: flex; gap: 8px; align-items: center">
            <el-button size="small" type="primary" plain :loading="checking" @click="onCheckResource">
              资源核查
            </el-button>
            <el-tag
              v-if="checkResult"
              :type="checkResult.status === 'RESOURCE_OK' ? 'success' : (checkResult.status === 'RESOURCE_INSUFFICIENT' ? 'warning' : 'danger')"
            >
              {{ checkResult.status === 'RESOURCE_OK' ? '可安装' : (checkResult.status === 'RESOURCE_INSUFFICIENT' ? '资源不足' : '暂无覆盖') }}
            </el-tag>
            <span v-if="checkResult" class="text-muted" style="font-size: 12px">{{ checkResult.message }}</span>
          </div>
        </el-form-item>

        <el-form-item label="地图选点">
          <BBPMSMapPicker v-model="form.location" />
          <div class="text-muted mt-8" style="font-size: 12px">
            Optional — Click on the map to set the service location coordinates.
          </div>
        </el-form-item>

        <el-form-item label="预约时间" prop="appointmentAt">
          <el-date-picker
            v-model="form.appointmentAt"
            type="datetime"
            placeholder="选择预约时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">提交</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>
