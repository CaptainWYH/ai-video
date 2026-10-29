import { theme, type ThemeConfig } from 'antd';

const FONT_FAMILY =
  '"Noto Sans SC", -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif';

const sharedToken = {
  borderRadius: 10,
  controlHeight: 40,
  fontFamily: FONT_FAMILY,
};

/** 纸感日间：暖白纸底 + 墨色文字 + 熟赭强调 */
export const lightThemeConfig: ThemeConfig = {
  algorithm: theme.defaultAlgorithm,
  token: {
    ...sharedToken,
    colorPrimary: '#9E5C16',
    colorInfo: '#1C7F7A',
    colorBgBase: '#FAF7F2',
    colorBgContainer: '#FFFDF9',
    colorBgElevated: '#FFFDF9',
    colorBorder: '#E2DACD',
    colorBorderSecondary: '#EDE7DC',
    colorText: '#1A1712',
    colorTextSecondary: '#55504A',
    colorTextTertiary: '#6E6860',
    colorTextLightSolid: '#FFFBF4',
    colorLink: '#9E5C16',
    colorLinkHover: '#7E470E',
    colorSuccess: '#2E7D57',
    colorError: '#B44138',
  },
  components: {
    Layout: {
      headerBg: 'transparent',
      bodyBg: 'transparent',
    },
    Button: {
      controlHeightLG: 48,
      primaryShadow: 'none',
    },
    Modal: {
      contentBg: '#FFFDF9',
      headerBg: '#FFFDF9',
    },
    Select: {
      optionSelectedBg: '#F3E7D4',
      selectorBg: 'transparent',
    },
    Dropdown: {
      colorBgElevated: '#FFFDF9',
    },
  },
};

/** 深棕墨夜间：深棕墨底 + 米白文字 + 琥珀强调 */
export const darkThemeConfig: ThemeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    ...sharedToken,
    colorPrimary: '#E8B268',
    colorInfo: '#75D8D2',
    colorBgBase: '#14110E',
    colorBgContainer: '#1B1713',
    colorBgElevated: '#221D18',
    colorBorder: '#332C24',
    colorBorderSecondary: '#28221C',
    colorText: '#F4EFE6',
    colorTextSecondary: '#BDB4A6',
    colorTextTertiary: '#978E80',
    colorTextLightSolid: '#241808',
    colorLink: '#E8B268',
    colorLinkHover: '#FFC97E',
    colorSuccess: '#6BD3A0',
    colorError: '#FF7A70',
  },
  components: {
    Layout: {
      headerBg: 'transparent',
      bodyBg: 'transparent',
    },
    Button: {
      controlHeightLG: 48,
      primaryShadow: 'none',
    },
    Modal: {
      contentBg: '#221D18',
      headerBg: '#221D18',
    },
    Select: {
      optionSelectedBg: '#33291C',
      selectorBg: 'transparent',
    },
    Dropdown: {
      colorBgElevated: '#221D18',
    },
  },
};
