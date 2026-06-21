/**
 * Excel 导出工具函数
 */
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

/**
 * 将数据导出为 Excel 文件
 * @param {Array} data - 要导出的数据数组
 * @param {Array} columns - 列配置 [{prop: 'id', label: 'ID'}, ...]
 * @param {string} filename - 文件名（不含扩展名）
 */
export function exportToExcel(data, columns, filename = 'export') {
  if (!data || data.length === 0) {
    return { success: false, message: '没有数据可导出' }
  }

  try {
    // 构建表头
    const headers = columns.map(col => col.label)
    
    // 构建数据行
    const rows = data.map(item => {
      return columns.map(col => {
        let value = item[col.prop]
        
        // 处理特殊字段的转换
        if (col.formatter) {
          value = col.formatter(value, item)
        }
        
        // 处理 null/undefined
        if (value === null || value === undefined) {
          return ''
        }
        
        return value
      })
    })
    
    // 合并表头和数据
    const sheetData = [headers, ...rows]
    
    // 创建工作表
    const worksheet = XLSX.utils.aoa_to_sheet(sheetData)
    
    // 设置列宽
    const colWidths = columns.map(col => ({
      wch: Math.max(col.label.length * 2, 15)
    }))
    worksheet['!cols'] = colWidths
    
    // 创建工作簿
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1')
    
    // 生成文件并下载
    const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([excelBuffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    
    // 添加时间戳
    const timestamp = new Date().toISOString().slice(0, 10)
    const fullFilename = `${filename}_${timestamp}.xlsx`
    
    saveAs(blob, fullFilename)
    
    return { success: true, message: '导出成功' }
  } catch (error) {
    console.error('导出失败:', error)
    return { success: false, message: '导出失败: ' + error.message }
  }
}

/**
 * 新闻导出列配置
 */
export const newsExportColumns = [
  { prop: 'id', label: '新闻ID' },
  { prop: 'title', label: '新闻标题' },
  { prop: 'category', label: '新闻分类' },
  { prop: 'author', label: '作者' },
  { 
    prop: 'status', 
    label: '状态',
    formatter: (value) => {
      const statusMap = { '0': '草稿', '1': '发布', '2': '下线' }
      return statusMap[value] || value
    }
  },
  { 
    prop: 'isTop', 
    label: '是否置顶',
    formatter: (value) => value === '1' ? '是' : '否'
  },
  { prop: 'viewCount', label: '浏览量' },
  { prop: 'publishTime', label: '发布时间' },
  { prop: 'createTime', label: '创建时间' }
]

/**
 * 作品导出列配置
 */
export const worksExportColumns = [
  { prop: 'id', label: '作品ID' },
  { prop: 'title', label: '作品标题' },
  { prop: 'description', label: '作品描述' },
  { 
    prop: 'category', 
    label: '作品分类',
    formatter: (value) => {
      const categoryMap = {
        'web': 'Web应用',
        'mobile': '移动应用',
        'ai': '人工智能',
        'desktop': '桌面应用',
        'other': '其他'
      }
      return categoryMap[value] || value
    }
  },
  { prop: 'authors', label: '作者' },
  { 
    prop: 'featured', 
    label: '是否精选',
    formatter: (value) => value === 1 || value === '1' ? '是' : '否'
  },
  { 
    prop: 'status', 
    label: '状态',
    formatter: (value) => value === 1 || value === '1' ? '显示' : '隐藏'
  },
  { prop: 'viewCount', label: '浏览量' },
  { prop: 'likeCount', label: '点赞数' },
  { prop: 'createTime', label: '创建时间' }
]

/**
 * 比赛导出列配置
 */
export const competitionExportColumns = [
  { prop: 'id', label: '比赛ID' },
  { prop: 'title', label: '比赛标题' },
  { prop: 'competitionType', label: '比赛类型' },
  { prop: 'level', label: '比赛级别' },
  { prop: 'organizer', label: '主办方' },
  { 
    prop: 'status', 
    label: '状态',
    formatter: (value) => {
      const statusMap = {
        'UPCOMING': '即将开始',
        'ONGOING': '进行中',
        'COMPLETED': '已结束',
        'CANCELLED': '已取消'
      }
      return statusMap[value] || value
    }
  },
  { prop: 'priority', label: '优先级' },
  { prop: 'registrationCount', label: '报名人数' },
  { prop: 'viewCount', label: '浏览数' },
  { prop: 'createTime', label: '创建时间' }
]
