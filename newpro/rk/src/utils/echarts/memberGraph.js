import * as echarts from 'echarts/core'
import { GraphChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  GraphChart,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer
])

export { echarts }
