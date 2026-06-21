function normalizePorts(ports) {
  if (Array.isArray(ports)) {
    return ports.filter(Boolean).map((item) => {
      if (typeof item === 'object') {
        return `${item.containerPort || item.port || ''}/${item.protocol || 'TCP'}`
      }
      return String(item)
    }).filter((item) => item && item !== '/TCP')
  }
  return String(ports || '')
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item && item !== '-')
}

function addUnique(map, node) {
  if (!map.has(node.id)) {
    map.set(node.id, node)
  }
}

export function buildK8sTopologyGraph({ nodes = [], pods = [], clusterName = 'rk-web' } = {}) {
  const graphNodes = new Map()
  const links = []
  const clusterId = `cluster:${clusterName}`

  addUnique(graphNodes, {
    id: clusterId,
    name: clusterName,
    category: 0,
    symbolSize: 74,
    value: 'K8s Cluster',
    itemStyle: { color: '#0f766e' }
  })

  nodes.forEach((node) => {
    const id = `node:${node.name || node.internalIp || 'unknown'}`
    addUnique(graphNodes, {
      id,
      name: node.name || node.internalIp || 'Unknown Node',
      category: 1,
      symbolSize: 58,
      value: node.internalIp || '',
      itemStyle: { color: node.status === 'Ready' ? '#18a058' : '#d03050' },
      raw: node
    })
    links.push({ source: clusterId, target: id, value: node.roles || 'node' })
  })

  pods.forEach((pod) => {
    const namespace = pod.namespace || 'default'
    const podName = pod.podName || pod.name || 'unknown-pod'
    const podId = `pod:${namespace}/${podName}`
    const nodeId = `node:${pod.nodeName || 'unassigned'}`
    if (!graphNodes.has(nodeId)) {
      addUnique(graphNodes, {
        id: nodeId,
        name: pod.nodeName || 'Unscheduled Node',
        category: 1,
        symbolSize: 50,
        value: '',
        itemStyle: { color: '#909399' }
      })
      links.push({ source: clusterId, target: nodeId, value: 'unassigned' })
    }
    addUnique(graphNodes, {
      id: podId,
      name: podName,
      category: 2,
      symbolSize: 42,
      value: namespace,
      itemStyle: { color: pod.status === 'Running' ? '#409eff' : '#e6a23c' },
      raw: pod
    })
    links.push({ source: nodeId, target: podId, value: namespace })

    const containerName = pod.containerName || 'container'
    const containerId = `container:${namespace}/${podName}/${containerName}`
    addUnique(graphNodes, {
      id: containerId,
      name: containerName,
      category: 3,
      symbolSize: 30,
      value: pod.image || '',
      itemStyle: { color: '#7c3aed' },
      raw: pod
    })
    links.push({ source: podId, target: containerId, value: pod.image || '' })

    normalizePorts(pod.ports).forEach((port) => {
      const portId = `port:${namespace}/${podName}/${containerName}/${port}`
      addUnique(graphNodes, {
        id: portId,
        name: port,
        category: 4,
        symbolSize: 20,
        value: port,
        itemStyle: { color: '#f59e0b' },
        raw: pod
      })
      links.push({ source: containerId, target: portId, value: port })
    })
  })

  return {
    categories: [
      { name: 'Cluster' },
      { name: 'Node' },
      { name: 'Pod' },
      { name: 'Container' },
      { name: 'Port' }
    ],
    nodes: Array.from(graphNodes.values()),
    links
  }
}

export function buildK8sTopologyOption(graph) {
  return {
    tooltip: {
      trigger: 'item',
      formatter(params) {
        const data = params.data || {}
        if (params.dataType === 'edge') {
          return `${params.data.source} -> ${params.data.target}`
        }
        const raw = data.raw || {}
        return [
          `<strong>${data.name}</strong>`,
          data.value ? `信息: ${data.value}` : '',
          raw.namespace ? `命名空间: ${raw.namespace}` : '',
          raw.podName ? `Pod: ${raw.podName}` : '',
          raw.containerName ? `容器: ${raw.containerName}` : '',
          raw.status ? `状态: ${raw.status}` : '',
          raw.restartCount != null ? `重启: ${raw.restartCount}` : '',
          raw.podIp ? `Pod IP: ${raw.podIp}` : '',
          raw.ports ? `端口: ${raw.ports}` : ''
        ].filter(Boolean).join('<br/>')
      }
    },
    legend: [{ data: graph.categories.map((item) => item.name), top: 0 }],
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        categories: graph.categories,
        data: graph.nodes,
        links: graph.links,
        label: {
          show: true,
          position: 'right',
          formatter: '{b}'
        },
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: 8,
        force: {
          repulsion: 300,
          edgeLength: [70, 160],
          gravity: 0.06
        },
        lineStyle: {
          color: 'source',
          curveness: 0.18,
          opacity: 0.76
        },
        emphasis: {
          focus: 'adjacency'
        }
      }
    ]
  }
}
