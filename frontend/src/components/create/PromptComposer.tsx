import { ArrowRightOutlined } from '@ant-design/icons';
import { Button, Input, Select } from 'antd';
import type { KeyboardEvent } from 'react';
import type { DraftParams } from '@/stores/draftStore';

interface PromptComposerProps {
  value: string;
  params: DraftParams;
  guest: boolean;
  loading: boolean;
  error?: string | null;
  onChange: (value: string) => void;
  onParamsChange: (patch: Partial<DraftParams>) => void;
  onGenerate: () => void;
}

const STYLE_OPTIONS = [
  { value: 'cinematic', label: '电影感' },
  { value: 'realistic', label: '写实' },
  { value: 'anime', label: '动漫' },
  { value: 'cyberpunk', label: '赛博朋克' },
  { value: 'ink', label: '水墨' },
];

const ASPECT_OPTIONS = [
  { value: '16:9', label: '16:9 横屏' },
  { value: '9:16', label: '9:16 竖屏' },
  { value: '1:1', label: '1:1 方形' },
  { value: '21:9', label: '21:9 宽银幕' },
];

const DURATION_OPTIONS = [
  { value: 5, label: '5 秒' },
  { value: 10, label: '10 秒' },
  { value: 15, label: '15 秒' },
  { value: 30, label: '30 秒' },
];

const RESOLUTION_OPTIONS = [
  { value: '720p', label: '720p' },
  { value: '1080p', label: '1080p' },
];

export default function PromptComposer({
  value,
  params,
  guest,
  loading,
  error,
  onChange,
  onParamsChange,
  onGenerate,
}: PromptComposerProps) {
  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter' || event.shiftKey) {
      return;
    }
    if (event.nativeEvent.isComposing) {
      return;
    }
    event.preventDefault();
    onGenerate();
  };

  return (
    <div className="composer">
      <Input.TextArea
        className="composer-textarea"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="描述你想看到的画面：海边日落，镜头缓慢推进，暖色调，海浪拍打礁石……"
        autoSize={{ minRows: 2, maxRows: 7 }}
        maxLength={2000}
        aria-label="视频画面描述"
      />

      <div className="composer-foot">
        <div className="composer-params" aria-label="生成参数">
          <span className="composer-param">
            <Select
              value={params.style}
              options={STYLE_OPTIONS}
              variant="borderless"
              popupMatchSelectWidth={false}
              aria-label="视觉风格"
              onChange={(style) => onParamsChange({ style })}
            />
          </span>
          <span className="composer-param">
            <Select
              value={params.aspect}
              options={ASPECT_OPTIONS}
              variant="borderless"
              popupMatchSelectWidth={false}
              aria-label="画面比例"
              onChange={(aspect) => onParamsChange({ aspect })}
            />
          </span>
          <span className="composer-param">
            <Select
              value={params.duration}
              options={DURATION_OPTIONS}
              variant="borderless"
              popupMatchSelectWidth={false}
              aria-label="视频时长"
              onChange={(duration) => onParamsChange({ duration })}
            />
          </span>
          <span className="composer-param">
            <Select
              value={params.resolution}
              options={RESOLUTION_OPTIONS}
              variant="borderless"
              popupMatchSelectWidth={false}
              aria-label="分辨率"
              onChange={(resolution) => onParamsChange({ resolution })}
            />
          </span>
        </div>

        <Button
          type="primary"
          className="composer-submit"
          loading={loading}
          disabled={!value.trim() || loading}
          onClick={onGenerate}
        >
          生成视频
          <ArrowRightOutlined />
        </Button>
      </div>

      {error ? <div className="composer-error">{error}</div> : null}

      <div className="composer-hint">
        <span>{guest ? '草稿已暂存 · 登录后同步到账号' : '分镜会随对话一起保存'}</span>
        <span>Enter 生成 · Shift + Enter 换行</span>
      </div>
    </div>
  );
}
