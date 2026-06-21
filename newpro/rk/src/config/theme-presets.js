export const THEME_PRESETS = {
  default: {
    key: 'default',
    label: '默认',
    description: '温润学院风，适合当前默认站点',
    preview: {
      primary: '#1f3a34',
      accent: '#c06b3e',
      surface: '#fbf7f0',
      text: '#1c2522'
    },
    frontend: {
      '--rk-primary': '#1f3a34',
      '--rk-primary-dark': '#162a25',
      '--rk-accent': '#c06b3e',
      '--rk-body-bg': '#f7f3ec',
      '--rk-surface-bg': '#fbf7f0',
      '--rk-surface-soft': '#f3efe8',
      '--rk-text-primary': '#1c2522',
      '--rk-text-secondary': '#5f6b67',
      '--rk-border-color': '#d7d0c6',
      '--rk-header-gradient': 'linear-gradient(135deg, #1f3a34 0%, #2d564d 100%)',
      '--rk-auth-gradient': 'linear-gradient(135deg, #003366 0%, #0066cc 100%)',
      '--rk-profile-gradient': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      '--rk-shadow-color': 'rgba(24, 33, 31, 0.12)'
    },
    admin: {
      '--rk-primary': '#1f3a34',
      '--rk-primary-dark': '#162a25',
      '--rk-accent': '#8c5e3c',
      '--rk-body-bg': '#f4efe7',
      '--rk-surface-bg': '#fffaf3',
      '--rk-surface-soft': '#f3ede4',
      '--rk-text-primary': '#1c2522',
      '--rk-text-secondary': '#5f6b67',
      '--rk-border-color': '#ddd4c9',
      '--rk-header-gradient': 'linear-gradient(135deg, #1f3a34 0%, #24483f 100%)',
      '--rk-admin-sidebar-bg': '#17231f',
      '--rk-admin-sidebar-header': '#22322d',
      '--rk-admin-sidebar-active': '#2f5d52',
      '--rk-admin-header-bg': '#fffaf3',
      '--rk-admin-body-bg': 'linear-gradient(180deg, #f4f8fd 0%, #ecf2f9 100%)',
      '--rk-shadow-color': 'rgba(24, 33, 31, 0.1)'
    }
  },
  gov_blue: {
    key: 'gov_blue',
    label: '政务蓝',
    description: '清晰稳重，适合正式组织与机构场景',
    preview: {
      primary: '#1d5fa7',
      accent: '#7bb4f0',
      surface: '#f4f8fd',
      text: '#0e2b4d'
    },
    frontend: {
      '--rk-primary': '#1d5fa7',
      '--rk-primary-dark': '#15487f',
      '--rk-accent': '#2f89d9',
      '--rk-body-bg': '#edf4fb',
      '--rk-surface-bg': '#f8fbff',
      '--rk-surface-soft': '#eaf2fb',
      '--rk-text-primary': '#17314f',
      '--rk-text-secondary': '#4a6787',
      '--rk-border-color': '#c9d7e8',
      '--rk-header-gradient': 'linear-gradient(135deg, #1d5fa7 0%, #3b88d6 100%)',
      '--rk-auth-gradient': 'linear-gradient(135deg, #184d88 0%, #2f89d9 100%)',
      '--rk-profile-gradient': 'linear-gradient(135deg, #1d5fa7 0%, #5ca3e6 100%)',
      '--rk-shadow-color': 'rgba(21, 72, 127, 0.12)'
    },
    admin: {
      '--rk-primary': '#1d5fa7',
      '--rk-primary-dark': '#15487f',
      '--rk-accent': '#5ca3e6',
      '--rk-body-bg': '#edf3fb',
      '--rk-surface-bg': '#ffffff',
      '--rk-surface-soft': '#eef4fb',
      '--rk-text-primary': '#17314f',
      '--rk-text-secondary': '#4a6787',
      '--rk-border-color': '#c9d7e8',
      '--rk-header-gradient': 'linear-gradient(135deg, #1d5fa7 0%, #3b88d6 100%)',
      '--rk-admin-sidebar-bg': '#143a66',
      '--rk-admin-sidebar-header': '#1a4a80',
      '--rk-admin-sidebar-active': '#2f6fb3',
      '--rk-admin-header-bg': '#ffffff',
      '--rk-admin-body-bg': 'linear-gradient(180deg, #edf3fb 0%, #dde8f5 100%)',
      '--rk-shadow-color': 'rgba(29, 95, 167, 0.1)'
    }
  },
  tech_dark: {
    key: 'tech_dark',
    label: '科技深色',
    description: '冷峻对比，适合技术型租户',
    preview: {
      primary: '#1de9b6',
      accent: '#5cc8ff',
      surface: '#10161d',
      text: '#e7f7ff'
    },
    frontend: {
      '--rk-primary': '#1de9b6',
      '--rk-primary-dark': '#12b58d',
      '--rk-accent': '#5cc8ff',
      '--rk-body-bg': '#091018',
      '--rk-surface-bg': '#111923',
      '--rk-surface-soft': '#162230',
      '--rk-text-primary': '#e7f7ff',
      '--rk-text-secondary': '#9db5c7',
      '--rk-border-color': '#223244',
      '--rk-header-gradient': 'linear-gradient(135deg, #0f202c 0%, #203a43 50%, #2c5364 100%)',
      '--rk-auth-gradient': 'linear-gradient(135deg, #0c1b2a 0%, #123048 100%)',
      '--rk-profile-gradient': 'linear-gradient(135deg, #122033 0%, #19344a 100%)',
      '--rk-shadow-color': 'rgba(0, 0, 0, 0.35)'
    },
    admin: {
      '--rk-primary': '#1de9b6',
      '--rk-primary-dark': '#12b58d',
      '--rk-accent': '#5cc8ff',
      '--rk-body-bg': '#0b1118',
      '--rk-surface-bg': '#131b24',
      '--rk-surface-soft': '#1b2734',
      '--rk-text-primary': '#e7f7ff',
      '--rk-text-secondary': '#9db5c7',
      '--rk-border-color': '#233344',
      '--rk-header-gradient': 'linear-gradient(135deg, #10202f 0%, #183247 100%)',
      '--rk-admin-sidebar-bg': '#08111a',
      '--rk-admin-sidebar-header': '#0f1c2a',
      '--rk-admin-sidebar-active': '#114b58',
      '--rk-admin-header-bg': '#131b24',
      '--rk-admin-body-bg': 'linear-gradient(180deg, #0b1118 0%, #0a0e14 100%)',
      '--rk-shadow-color': 'rgba(0, 0, 0, 0.35)'
    }
  },
  warm_business: {
    key: 'warm_business',
    label: '暖色商务',
    description: '柔和暖调，适合社团展示与运营后台',
    preview: {
      primary: '#9a5035',
      accent: '#d8a15b',
      surface: '#fff6eb',
      text: '#3b2418'
    },
    frontend: {
      '--rk-primary': '#9a5035',
      '--rk-primary-dark': '#6f3926',
      '--rk-accent': '#d8a15b',
      '--rk-body-bg': '#f8efe5',
      '--rk-surface-bg': '#fff8ef',
      '--rk-surface-soft': '#f7ecdf',
      '--rk-text-primary': '#3b2418',
      '--rk-text-secondary': '#735645',
      '--rk-border-color': '#e1c8b3',
      '--rk-header-gradient': 'linear-gradient(135deg, #9a5035 0%, #c47a4f 100%)',
      '--rk-auth-gradient': 'linear-gradient(135deg, #8f4b31 0%, #c47a4f 100%)',
      '--rk-profile-gradient': 'linear-gradient(135deg, #9a5035 0%, #d28f5e 100%)',
      '--rk-shadow-color': 'rgba(59, 36, 24, 0.14)'
    },
    admin: {
      '--rk-primary': '#9a5035',
      '--rk-primary-dark': '#6f3926',
      '--rk-accent': '#d8a15b',
      '--rk-body-bg': '#f7ede3',
      '--rk-surface-bg': '#fffaf4',
      '--rk-surface-soft': '#f6ebdf',
      '--rk-text-primary': '#3b2418',
      '--rk-text-secondary': '#735645',
      '--rk-border-color': '#e1c8b3',
      '--rk-header-gradient': 'linear-gradient(135deg, #9a5035 0%, #c47a4f 100%)',
      '--rk-admin-sidebar-bg': '#442a1f',
      '--rk-admin-sidebar-header': '#5c3829',
      '--rk-admin-sidebar-active': '#8e5337',
      '--rk-admin-header-bg': '#fffaf4',
      '--rk-admin-body-bg': 'linear-gradient(180deg, #f7ede3 0%, #f0e3d5 100%)',
      '--rk-shadow-color': 'rgba(59, 36, 24, 0.14)'
    }
  }
}

export const THEME_PRESET_LIST = Object.values(THEME_PRESETS)
