import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export function useBatchDelete(options) {
  const {
    deleteItem,
    fetchList,
    entityName = '记录',
    getId = (row) => row?.id,
    confirmTitle = `批量删除${entityName}`
  } = options

  const selectedRows = ref([])

  const handleSelectionChange = (rows = []) => {
    selectedRows.value = rows
  }

  const handleBatchDelete = async () => {
    if (!selectedRows.value.length) {
      ElMessage.warning(`请先选择要删除的${entityName}`)
      return
    }

    const rows = [...selectedRows.value]
    await ElMessageBox.confirm(
      `确定删除选中的 ${rows.length} 条${entityName}吗？删除后不可恢复。`,
      confirmTitle,
      { type: 'warning' }
    )

    const results = await Promise.allSettled(
      rows.map((row) => deleteItem(getId(row), row))
    )
    const successCount = results.filter((item) => item.status === 'fulfilled').length
    const failureCount = results.length - successCount

    selectedRows.value = []
    await fetchList?.()

    if (failureCount > 0) {
      ElMessage.warning(`已删除 ${successCount} 条，${failureCount} 条删除失败`)
      return
    }

    ElMessage.success(`已删除 ${successCount} 条${entityName}`)
  }

  return {
    selectedRows,
    handleSelectionChange,
    handleBatchDelete
  }
}
