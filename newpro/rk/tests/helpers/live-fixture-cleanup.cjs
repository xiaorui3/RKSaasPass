// @ts-check
const mysql = require('mysql2/promise')

const DB_CONFIG = {
  host: process.env.PLAYWRIGHT_DB_HOST || '127.0.0.1',
  port: Number(process.env.PLAYWRIGHT_DB_PORT || 3306),
  user: process.env.PLAYWRIGHT_DB_USER || 'root',
  password: process.env.PLAYWRIGHT_DB_PASSWORD || '123'
}

async function withConnection(work) {
  const connection = await mysql.createConnection(DB_CONFIG)
  try {
    return await work(connection)
  } finally {
    await connection.end()
  }
}

async function cleanupAlumniFixture({ tenantId = 1, name, email }) {
  await withConnection(async (connection) => {
    await connection.execute(
      `
      DELETE FROM rk_user.club_alumni
      WHERE tenant_id = ?
        AND (
          (? IS NOT NULL AND name = ?)
          OR (? IS NOT NULL AND email = ?)
        )
      `,
      [tenantId, name ?? null, name ?? null, email ?? null, email ?? null]
    )
  })
}

async function cleanupAdmissionFixture({ tenantId = 1, studentId, username, email }) {
  await withConnection(async (connection) => {
    await connection.beginTransaction()
    try {
      const [localUsers] = await connection.execute(
        `
        SELECT id, auth_user_id
        FROM rk_user.rk_user
        WHERE tenant_id = ?
          AND (
            (? IS NOT NULL AND student_id = ?)
            OR (? IS NOT NULL AND username = ?)
            OR (? IS NOT NULL AND email = ?)
          )
        `,
        [
          tenantId,
          studentId ?? null,
          studentId ?? null,
          username ?? null,
          username ?? null,
          email ?? null,
          email ?? null
        ]
      )

      const localUserIds = localUsers.map((row) => Number(row.id)).filter(Number.isFinite)
      const authUserIds = localUsers.map((row) => Number(row.auth_user_id)).filter(Number.isFinite)

      await connection.execute(
        `
        DELETE FROM rk_user.club_members
        WHERE tenant_id = ?
          AND (
            (? IS NOT NULL AND student_id = ?)
            OR (? IS NOT NULL AND email = ?)
          )
        `,
        [tenantId, studentId ?? null, studentId ?? null, email ?? null, email ?? null]
      )

      await connection.execute(
        `
        DELETE FROM rk_user.join_requests
        WHERE tenant_id = ?
          AND (
            (? IS NOT NULL AND student_id = ?)
            OR (? IS NOT NULL AND username = ?)
            OR (? IS NOT NULL AND email = ?)
          )
        `,
        [
          tenantId,
          studentId ?? null,
          studentId ?? null,
          username ?? null,
          username ?? null,
          email ?? null,
          email ?? null
        ]
      )

      if (localUserIds.length > 0) {
        await connection.query(
          'DELETE FROM rk_user.rk_user_detail WHERE user_id IN (?)',
          [localUserIds]
        )
        await connection.query(
          'DELETE FROM rk_user.rk_user WHERE id IN (?)',
          [localUserIds]
        )
      }

      if (authUserIds.length > 0) {
        await connection.query(
          'DELETE FROM rk_auth.account_role WHERE account_id IN (?)',
          [authUserIds]
        )
        await connection.query(
          'DELETE FROM rk_auth.login_record WHERE user_id IN (?)',
          [authUserIds]
        )
        await connection.query(
          'DELETE FROM rk_auth.user WHERE id IN (?)',
          [authUserIds]
        )
      } else if (username) {
        const [authUsers] = await connection.execute(
          'SELECT id FROM rk_auth.user WHERE tenant_id = ? AND username = ?',
          [tenantId, username]
        )
        const fallbackAuthIds = authUsers.map((row) => Number(row.id)).filter(Number.isFinite)
        if (fallbackAuthIds.length > 0) {
          await connection.query(
            'DELETE FROM rk_auth.account_role WHERE account_id IN (?)',
            [fallbackAuthIds]
          )
          await connection.query(
            'DELETE FROM rk_auth.login_record WHERE user_id IN (?)',
            [fallbackAuthIds]
          )
          await connection.query(
            'DELETE FROM rk_auth.user WHERE id IN (?)',
            [fallbackAuthIds]
          )
        }
      }

      await connection.commit()
    } catch (error) {
      await connection.rollback()
      throw error
    }
  })
}

async function cleanupManagedUserFixture({ tenantId = 1, username, email, studentId }) {
  await withConnection(async (connection) => {
    await connection.beginTransaction()
    try {
      const [localUsers] = await connection.execute(
        `
        SELECT id, auth_user_id
        FROM rk_user.rk_user
        WHERE tenant_id = ?
          AND (
            (? IS NOT NULL AND student_id = ?)
            OR (? IS NOT NULL AND username = ?)
            OR (? IS NOT NULL AND email = ?)
          )
        `,
        [
          tenantId,
          studentId ?? null,
          studentId ?? null,
          username ?? null,
          username ?? null,
          email ?? null,
          email ?? null
        ]
      )

      const localUserIds = localUsers.map((row) => Number(row.id)).filter(Number.isFinite)
      const authUserIds = localUsers.map((row) => Number(row.auth_user_id)).filter(Number.isFinite)

      await connection.execute(
        `
        DELETE FROM rk_user.club_members
        WHERE tenant_id = ?
          AND (
            (? IS NOT NULL AND student_id = ?)
            OR (? IS NOT NULL AND email = ?)
          )
        `,
        [tenantId, studentId ?? null, studentId ?? null, email ?? null, email ?? null]
      )

      if (localUserIds.length > 0) {
        await connection.query(
          'DELETE FROM rk_user.rk_user_detail WHERE user_id IN (?)',
          [localUserIds]
        )
        await connection.query(
          'DELETE FROM rk_user.rk_user WHERE id IN (?)',
          [localUserIds]
        )
      }

      if (authUserIds.length > 0) {
        await connection.query(
          'DELETE FROM rk_auth.account_role WHERE account_id IN (?)',
          [authUserIds]
        )
        await connection.query(
          'DELETE FROM rk_auth.login_record WHERE user_id IN (?)',
          [authUserIds]
        )
        await connection.query(
          'DELETE FROM rk_auth.user WHERE id IN (?)',
          [authUserIds]
        )
      } else if (username) {
        const [authUsers] = await connection.execute(
          'SELECT id FROM rk_auth.user WHERE tenant_id = ? AND username = ?',
          [tenantId, username]
        )
        const fallbackAuthIds = authUsers.map((row) => Number(row.id)).filter(Number.isFinite)
        if (fallbackAuthIds.length > 0) {
          await connection.query(
            'DELETE FROM rk_auth.account_role WHERE account_id IN (?)',
            [fallbackAuthIds]
          )
          await connection.query(
            'DELETE FROM rk_auth.login_record WHERE user_id IN (?)',
            [fallbackAuthIds]
          )
          await connection.query(
            'DELETE FROM rk_auth.user WHERE id IN (?)',
            [fallbackAuthIds]
          )
        }
      }

      await connection.commit()
    } catch (error) {
      await connection.rollback()
      throw error
    }
  })
}

async function cleanupRegisterFixture({ tenantId = 1, username, email, studentId }) {
  await withConnection(async (connection) => {
    await connection.beginTransaction()
    try {
      const [localUsers] = await connection.execute(
        `
        SELECT id, auth_user_id
        FROM rk_user.rk_user
        WHERE tenant_id = ?
          AND (
            (? IS NOT NULL AND student_id = ?)
            OR
            (? IS NOT NULL AND username = ?)
            OR (? IS NOT NULL AND email = ?)
          )
        `,
        [
          tenantId,
          studentId ?? null,
          studentId ?? null,
          username ?? null,
          username ?? null,
          email ?? null,
          email ?? null
        ]
      )

      const localUserIds = localUsers.map((row) => Number(row.id)).filter(Number.isFinite)
      const authUserIds = localUsers.map((row) => Number(row.auth_user_id)).filter(Number.isFinite)

      await connection.execute(
        `
        DELETE FROM rk_user.club_members
        WHERE tenant_id = ?
          AND (
            (? IS NOT NULL AND student_id = ?)
            OR (? IS NOT NULL AND email = ?)
          )
        `,
        [tenantId, studentId ?? null, studentId ?? null, email ?? null, email ?? null]
      )

      await connection.execute(
        `
        DELETE FROM rk_user.register_review_request
        WHERE tenant_id = ?
          AND (
            (? IS NOT NULL AND username = ?)
            OR (? IS NOT NULL AND email = ?)
          )
        `,
        [
          tenantId,
          username ?? null,
          username ?? null,
          email ?? null,
          email ?? null
        ]
      )

      if (localUserIds.length > 0) {
        await connection.query('DELETE FROM rk_user.rk_user_detail WHERE user_id IN (?)', [localUserIds])
        await connection.query('DELETE FROM rk_user.rk_user WHERE id IN (?)', [localUserIds])
      }

      if (authUserIds.length > 0) {
        await connection.query('DELETE FROM rk_auth.account_role WHERE account_id IN (?)', [authUserIds])
        await connection.query('DELETE FROM rk_auth.login_record WHERE user_id IN (?)', [authUserIds])
        await connection.query('DELETE FROM rk_auth.user WHERE id IN (?)', [authUserIds])
      } else if (username) {
        const [authUsers] = await connection.execute(
          'SELECT id FROM rk_auth.user WHERE tenant_id = ? AND username = ?',
          [tenantId, username]
        )
        const fallbackAuthIds = authUsers.map((row) => Number(row.id)).filter(Number.isFinite)
        if (fallbackAuthIds.length > 0) {
          await connection.query('DELETE FROM rk_auth.account_role WHERE account_id IN (?)', [fallbackAuthIds])
          await connection.query('DELETE FROM rk_auth.login_record WHERE user_id IN (?)', [fallbackAuthIds])
          await connection.query('DELETE FROM rk_auth.user WHERE id IN (?)', [fallbackAuthIds])
        }
      }

      await connection.commit()
    } catch (error) {
      await connection.rollback()
      throw error
    }
  })
}

module.exports = {
  cleanupAdmissionFixture,
  cleanupAlumniFixture,
  cleanupManagedUserFixture,
  cleanupRegisterFixture
}
