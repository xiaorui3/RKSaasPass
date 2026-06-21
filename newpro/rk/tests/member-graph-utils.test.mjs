import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildMemberGraphData,
  buildMemberGraphSummary,
  extractMemberGraphFilterOptions,
  filterMemberGraphPeople
} from '../src/utils/memberGraph.js'

const members = [
  {
    id: 1,
    name: '张三',
    status: '正常',
    position: '普通成员',
    department: '宣传部',
    college: '计算机学院',
    grade: '2023',
    gender: '男'
  },
  {
    id: 2,
    name: '李四',
    status: '正常',
    position: '指导老师',
    department: '指导组',
    college: '信息学院',
    grade: '教师',
    gender: '女'
  }
]

const alumni = [
  {
    id: 11,
    name: '王五',
    department: '宣传部',
    generationYear: '2019',
    college: '计算机学院',
    gender: '男',
    graduationStatus: '已毕业'
  }
]

test('extractMemberGraphFilterOptions collects readable filter values', () => {
  const options = extractMemberGraphFilterOptions(members, alumni)

  assert.deepEqual(options.departments, ['宣传部', '指导组'])
  assert.deepEqual(options.colleges, ['计算机学院', '信息学院'])
  assert.deepEqual(options.grades, ['2019届', '2023', '教师'])
  assert.deepEqual(options.genders, ['男', '女'])
})

test('filterMemberGraphPeople keeps member and alumni scopes separated', () => {
  const filtered = filterMemberGraphPeople(
    members,
    alumni,
    {
      keyword: '张',
      personType: 'member',
      department: '',
      college: '',
      grade: '',
      gender: ''
    }
  )

  assert.equal(filtered.members.length, 1)
  assert.equal(filtered.members[0].name, '张三')
  assert.equal(filtered.alumni.length, 0)
})

test('buildMemberGraphSummary reports current split counts', () => {
  const summary = buildMemberGraphSummary(members, alumni)

  assert.deepEqual(summary, {
    totalPeople: 3,
    memberCount: 2,
    alumniCount: 1,
    departmentCount: 2,
    collegeCount: 2
  })
})

test('buildMemberGraphData creates two roots and person links', () => {
  const graph = buildMemberGraphData(members, alumni)

  assert.equal(graph.nodes.some((node) => node.name === '成员台账'), true)
  assert.equal(graph.nodes.some((node) => node.name === '校友档案'), true)
  assert.equal(graph.nodes.some((node) => node.name === '张三'), true)
  assert.equal(graph.nodes.some((node) => node.name === '王五'), true)
  assert.equal(graph.links.some((link) => link.target === 'member-root'), true)
  assert.equal(graph.links.some((link) => link.target === 'alumni-root'), true)
})
