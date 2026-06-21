function normalizeText(value) {
  return String(value || '').trim()
}

function buildAlumniGradeLabel(item) {
  const generationYear = normalizeText(item?.generationYear)
  if (!generationYear) {
    return ''
  }
  return generationYear.endsWith('届') ? generationYear : `${generationYear}届`
}

function buildMemberGroupName(member) {
  return normalizeText(member?.position)
    || normalizeText(member?.department)
    || '普通成员'
}

function buildAlumniGroupName(alumni) {
  return buildAlumniGradeLabel(alumni)
    || normalizeText(alumni?.department)
    || '校友档案'
}

function buildSearchHaystack(item, type) {
  const fields = [
    item?.name,
    item?.username,
    item?.studentId,
    item?.department,
    item?.college,
    item?.grade,
    item?.position,
    item?.gender
  ]
  if (type === 'alumni') {
    fields.push(item?.generationYear, buildAlumniGradeLabel(item), item?.graduationStatus)
  }
  return fields.map((value) => normalizeText(value).toLowerCase()).join(' ')
}

function matchesCommonFilters(item, type, filters) {
  const keyword = normalizeText(filters?.keyword).toLowerCase()
  if (keyword && !buildSearchHaystack(item, type).includes(keyword)) {
    return false
  }

  const department = normalizeText(filters?.department)
  if (department && normalizeText(item?.department) !== department) {
    return false
  }

  const college = normalizeText(filters?.college)
  if (college && normalizeText(item?.college) !== college) {
    return false
  }

  const gender = normalizeText(filters?.gender)
  if (gender && normalizeText(item?.gender) !== gender) {
    return false
  }

  const grade = normalizeText(filters?.grade)
  if (grade) {
    const currentGrade = type === 'alumni'
      ? buildAlumniGradeLabel(item)
      : normalizeText(item?.grade)
    if (currentGrade !== grade) {
      return false
    }
  }

  return true
}

function sortTextValues(values) {
  return [...values]
    .filter(Boolean)
    .sort((left, right) => left.localeCompare(right, 'zh-Hans-CN'))
}

export function extractMemberGraphFilterOptions(members = [], alumni = []) {
  const departments = new Set()
  const colleges = new Set()
  const grades = new Set()
  const genders = new Set()

  for (const member of members) {
    const department = normalizeText(member?.department)
    const college = normalizeText(member?.college)
    const grade = normalizeText(member?.grade)
    const gender = normalizeText(member?.gender)
    if (department) departments.add(department)
    if (college) colleges.add(college)
    if (grade) grades.add(grade)
    if (gender) genders.add(gender)
  }

  for (const item of alumni) {
    const department = normalizeText(item?.department)
    const college = normalizeText(item?.college)
    const grade = buildAlumniGradeLabel(item)
    const gender = normalizeText(item?.gender)
    if (department) departments.add(department)
    if (college) colleges.add(college)
    if (grade) grades.add(grade)
    if (gender) genders.add(gender)
  }

  return {
    departments: sortTextValues(departments),
    colleges: sortTextValues(colleges),
    grades: sortTextValues(grades),
    genders: sortTextValues(genders)
  }
}

export function filterMemberGraphPeople(members = [], alumni = [], filters = {}) {
  const personType = normalizeText(filters?.personType) || 'all'
  const filteredMembers = personType === 'alumni'
    ? []
    : members.filter((member) => matchesCommonFilters(member, 'member', filters))
  const filteredAlumni = personType === 'member'
    ? []
    : alumni.filter((item) => matchesCommonFilters(item, 'alumni', filters))

  return {
    members: filteredMembers,
    alumni: filteredAlumni
  }
}

export function buildMemberGraphSummary(members = [], alumni = []) {
  const departments = new Set()
  const colleges = new Set()

  for (const member of members) {
    const department = normalizeText(member?.department)
    const college = normalizeText(member?.college)
    if (department) departments.add(department)
    if (college) colleges.add(college)
  }

  for (const item of alumni) {
    const department = normalizeText(item?.department)
    const college = normalizeText(item?.college)
    if (department) departments.add(department)
    if (college) colleges.add(college)
  }

  return {
    totalPeople: members.length + alumni.length,
    memberCount: members.length,
    alumniCount: alumni.length,
    departmentCount: departments.size,
    collegeCount: colleges.size
  }
}

export function buildMemberGraphData(members = [], alumni = []) {
  const nodes = []
  const links = []
  const seenNodeIds = new Set()

  const pushNode = (node) => {
    if (seenNodeIds.has(node.id)) {
      return
    }
    seenNodeIds.add(node.id)
    nodes.push(node)
  }

  const rootId = 'club-root'
  const memberRootId = 'member-root'
  const alumniRootId = 'alumni-root'

  pushNode({
    id: rootId,
    name: '成员关系图',
    symbolSize: 72,
    itemStyle: { color: '#2563eb' },
    category: 0
  })
  pushNode({
    id: memberRootId,
    name: '成员台账',
    symbolSize: 58,
    itemStyle: { color: '#0f766e' },
    category: 1
  })
  pushNode({
    id: alumniRootId,
    name: '校友档案',
    symbolSize: 58,
    itemStyle: { color: '#7c3aed' },
    category: 1
  })

  links.push({ source: rootId, target: memberRootId })
  links.push({ source: rootId, target: alumniRootId })

  const memberGroups = new Set()
  for (const member of members) {
    const groupName = buildMemberGroupName(member)
    const groupId = `member-group-${groupName}`
    if (!memberGroups.has(groupId)) {
      memberGroups.add(groupId)
      pushNode({
        id: groupId,
        name: groupName,
        symbolSize: 38,
        itemStyle: { color: '#f59e0b' },
        category: 2
      })
      links.push({ source: memberRootId, target: groupId })
    }

    const personId = `member-${member.id || member.studentId || member.username || member.name}`
    pushNode({
      id: personId,
      name: normalizeText(member?.name) || normalizeText(member?.username) || normalizeText(member?.studentId) || '未命名成员',
      symbolSize: 24,
      itemStyle: { color: '#475569' },
      category: 3,
      label: { show: true, fontSize: 11 }
    })
    links.push({ source: groupId, target: personId })
  }

  const alumniGroups = new Set()
  for (const item of alumni) {
    const groupName = buildAlumniGroupName(item)
    const groupId = `alumni-group-${groupName}`
    if (!alumniGroups.has(groupId)) {
      alumniGroups.add(groupId)
      pushNode({
        id: groupId,
        name: groupName,
        symbolSize: 38,
        itemStyle: { color: '#a855f7' },
        category: 2
      })
      links.push({ source: alumniRootId, target: groupId })
    }

    const personId = `alumni-${item.id || item.studentId || item.email || item.name}`
    pushNode({
      id: personId,
      name: normalizeText(item?.name) || normalizeText(item?.studentId) || '未命名校友',
      symbolSize: 24,
      itemStyle: { color: '#64748b' },
      category: 3,
      label: { show: true, fontSize: 11 }
    })
    links.push({ source: groupId, target: personId })
  }

  return { nodes, links }
}
