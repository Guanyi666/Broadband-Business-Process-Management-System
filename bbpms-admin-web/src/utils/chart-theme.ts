/**
 * ECharts 统一配置工厂
 *
 * 目标：消灭各图表重复的 tooltip / legend / grid / axis 样板，统一色板与字体。
 * 约定：
 *  - 字体 12px，PingFang SC / Microsoft YaHei 优先
 *  - Tooltip：axis（折线/柱）或 item（饼/仪表），深底白字，confine 防溢出
 *  - 图例：底部居中；坐标轴：轴线 #dcdfe6，分割线虚线，标签 #909399
 *  - Y 轴单位必填（「单」「个」「分钟」等）
 *  - 动画 400ms，禁用 3D / 强渐变等视觉噪音
 */
import type { EChartsOption } from 'echarts'

/** 图表统一色板（与 Element Plus 主色系一致，按系列顺序取用） */
export const CHART_PALETTE = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#36cfc9'] as const

/** 图表字体 */
export const CHART_FONT_FAMILY = '-apple-system, PingFang SC, Microsoft YaHei, sans-serif'

const AXIS_LINE_COLOR = '#dcdfe6'
const SPLIT_LINE_COLOR = '#ebeef5'
const AXIS_LABEL_COLOR = '#909399'

export function baseTooltip(trigger: 'axis' | 'item' = 'axis'): EChartsOption['tooltip'] {
  return {
    trigger,
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    borderWidth: 0,
    padding: [8, 12],
    textStyle: { color: '#fff', fontSize: 12, fontFamily: CHART_FONT_FAMILY },
    confine: true
  }
}

export function baseLegend(data?: string[]): EChartsOption['legend'] {
  return {
    bottom: 0,
    left: 'center',
    itemWidth: 10,
    itemHeight: 10,
    itemGap: 16,
    icon: 'circle',
    textStyle: { color: '#606266', fontSize: 12, fontFamily: CHART_FONT_FAMILY },
    ...(data ? { data } : {})
  }
}

export function baseGrid(): EChartsOption['grid'] {
  return { left: 48, right: 24, top: 32, bottom: 32, containLabel: true }
}

export function axisStyle(name?: string): Record<string, unknown> {
  return {
    ...(name ? { name } : {}),
    nameTextStyle: { color: AXIS_LABEL_COLOR, fontSize: 12, fontFamily: CHART_FONT_FAMILY },
    axisLine: { lineStyle: { color: AXIS_LINE_COLOR } },
    axisTick: { show: false },
    axisLabel: { color: AXIS_LABEL_COLOR, fontSize: 12, fontFamily: CHART_FONT_FAMILY },
    splitLine: { lineStyle: { color: SPLIT_LINE_COLOR, type: 'dashed' } }
  }
}

export interface LineSeries {
  name: string
  data: number[]
  area?: boolean
}

/** 折线 / 面积图（多系列） */
export function buildLineOption(dates: string[], series: LineSeries[], unit?: string): EChartsOption {
  return {
    color: [...CHART_PALETTE],
    tooltip: baseTooltip('axis'),
    legend: baseLegend(series.map((s) => s.name)),
    grid: baseGrid(),
    xAxis: { type: 'category', boundaryGap: false, data: dates, ...axisStyle() },
    yAxis: { type: 'value', ...axisStyle(unit), minInterval: 1 },
    animationDuration: 400,
    series: series.map((s, i) => ({
      name: s.name,
      type: 'line' as const,
      smooth: true,
      showSymbol: false,
      data: s.data,
      lineStyle: { width: 2, color: CHART_PALETTE[i % CHART_PALETTE.length] },
      itemStyle: { color: CHART_PALETTE[i % CHART_PALETTE.length] },
      ...(s.area
        ? {
            areaStyle: {
              color: {
                type: 'linear' as const,
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  { offset: 0, color: `${CHART_PALETTE[i % CHART_PALETTE.length]}26` },
                  { offset: 1, color: `${CHART_PALETTE[i % CHART_PALETTE.length]}00` }
                ]
              }
            }
          }
        : {})
    }))
  }
}

/** 条形图（horizontal=true 时横向） */
export function buildBarOption(
  categories: string[],
  data: number[],
  unit?: string,
  horizontal = false
): EChartsOption {
  const valueAxis = { type: 'value' as const, ...axisStyle(unit), minInterval: 1 }
  const categoryAxis = { type: 'category' as const, data: categories, ...axisStyle() }
  return {
    color: [...CHART_PALETTE],
    tooltip: baseTooltip('axis'),
    grid: baseGrid(),
    xAxis: horizontal ? valueAxis : categoryAxis,
    yAxis: horizontal ? categoryAxis : valueAxis,
    animationDuration: 400,
    series: [
      {
        type: 'bar',
        data,
        barMaxWidth: 24,
        itemStyle: { color: CHART_PALETTE[0], borderRadius: horizontal ? [0, 4, 4, 0] : [4, 4, 0, 0] }
      }
    ]
  }
}

/** 环形图（占比场景） */
export function buildDonutOption(items: { name: string; value: number }[]): EChartsOption {
  return {
    color: [...CHART_PALETTE],
    tooltip: baseTooltip('item'),
    legend: baseLegend(items.map((i) => i.name)),
    animationDuration: 400,
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, formatter: '{b}: {c}', fontSize: 12, fontFamily: CHART_FONT_FAMILY } },
        data: items
      }
    ]
  }
}

/** 仪表盘（单值指标，如平均评分） */
export function buildGaugeOption(value: number, unit = '分', max = 5): EChartsOption {
  return {
    tooltip: baseTooltip('item'),
    animationDuration: 400,
    series: [
      {
        type: 'gauge',
        min: 0,
        max,
        radius: '90%',
        center: ['50%', '56%'],
        startAngle: 210,
        endAngle: -30,
        axisLine: { lineStyle: { width: 12, color: [[0.25, '#f56c6c'], [0.5, '#e6a23c'], [1, '#67c23a']] } },
        pointer: { length: '58%', width: 4, itemStyle: { color: '#909399' } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { color: AXIS_LABEL_COLOR, fontSize: 11, distance: 16, fontFamily: CHART_FONT_FAMILY },
        title: { show: true, offsetCenter: [0, '62%'], color: '#909399', fontSize: 12, fontFamily: CHART_FONT_FAMILY },
        detail: {
          valueAnimation: true,
          formatter: `{value}${unit}`,
          color: '#303133',
          fontSize: 22,
          fontWeight: 600,
          offsetCenter: [0, '32%'],
          fontFamily: CHART_FONT_FAMILY
        },
        data: [{ value, name: `平均派单评分（${unit}）` }]
      }
    ]
  }
}
