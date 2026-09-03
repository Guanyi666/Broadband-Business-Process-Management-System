<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createOrder } from '@/api/order'
import { createCustomer, searchCustomers } from '@/api/customer'
import type { Customer } from '@/api/customer'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSMapPicker from '@/components/BBPMSMapPicker.vue'

const router = useRouter()

const formRef = ref()
const submitting = ref(false)

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
  customerId: [{ required: true, message: 'Please select a customer', trigger: 'change' }],
  customerName: [{ required: true, message: 'Customer name is required', trigger: 'blur' }],
  customerPhone: [
    { required: true, message: 'Phone is required', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: 'Invalid phone', trigger: 'blur' }
  ],
  packageId: [{ required: true, message: 'Please select a package', trigger: 'change' }],
  address: [{ required: true, message: 'Address is required', trigger: 'blur' }],
  appointmentAt: [{ required: true, message: 'Appointment time is required', trigger: 'change' }]
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
        remark: form.remark
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
    <PageHeader title="Create Order" description="Submit a new broadband order" />

    <div class="app-card" style="max-width: 880px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="Customer">
          <el-radio-group v-model="form.customerMode">
            <el-radio-button value="EXISTING">Existing</el-radio-button>
            <el-radio-button value="NEW">New</el-radio-button>
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
              placeholder="Search by name or phone"
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
          <el-form-item label="Name" prop="customerName">
            <el-input v-model="form.customerName" />
          </el-form-item>
          <el-form-item label="Phone" prop="customerPhone">
            <el-input v-model="form.customerPhone" maxlength="11" />
          </el-form-item>
          <el-form-item label="ID Card">
            <el-input v-model="form.customerIdCard" maxlength="18" />
          </el-form-item>
        </template>

        <el-form-item label="Package" prop="packageId">
          <el-select v-model="form.packageId" placeholder="Select package" style="width: 100%">
            <el-option v-for="p in packageOptions" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="Address" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item label="Pick on Map">
          <BBPMSMapPicker v-model="form.location" />
          <div class="text-muted mt-8" style="font-size: 12px">
            Optional — Click on the map to set the service location coordinates.
          </div>
        </el-form-item>

        <el-form-item label="Appointment" prop="appointmentAt">
          <el-date-picker
            v-model="form.appointmentAt"
            type="datetime"
            placeholder="Select appointment time"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="Remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">Submit</el-button>
          <el-button @click="router.back()">Cancel</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>
