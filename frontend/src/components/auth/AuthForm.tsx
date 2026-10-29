import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Form, Input, message } from 'antd';
import { useEffect, useState } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { getErrorMessage } from '@/utils/error';

export type AuthMode = 'login' | 'register';

interface AuthFormValues {
  account?: string;
  username?: string;
  email?: string;
  nickname?: string;
  password: string;
  confirmPassword?: string;
}

interface AuthFormProps {
  mode: AuthMode;
  onSuccess: () => void | Promise<void>;
}

export default function AuthForm({ mode, onSuccess }: AuthFormProps) {
  const [form] = Form.useForm<AuthFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const login = useAuthStore((state) => state.login);
  const register = useAuthStore((state) => state.register);

  useEffect(() => {
    form.resetFields();
  }, [form, mode]);

  const handleFinish = async (values: AuthFormValues) => {
    setSubmitting(true);
    try {
      if (mode === 'login') {
        await login({
          account: values.account?.trim() ?? '',
          password: values.password,
        });
      } else {
        await register({
          username: values.username?.trim() ?? '',
          email: values.email?.trim() ?? '',
          password: values.password,
          nickname: values.nickname?.trim() || undefined,
        });
      }
      await onSuccess();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Form<AuthFormValues>
      form={form}
      layout="vertical"
      requiredMark={false}
      size="large"
      className="auth-form"
      onFinish={handleFinish}
    >
      {mode === 'login' ? (
        <Form.Item
          name="account"
          label="用户名或邮箱"
          rules={[{ required: true, message: '请输入用户名或邮箱' }]}
        >
          <Input
            prefix={<UserOutlined />}
            placeholder="请输入用户名或邮箱"
            autoComplete="username"
          />
        </Form.Item>
      ) : (
        <>
          <div className="auth-form-row">
            <Form.Item
              name="username"
              label="用户名"
              rules={[
                { required: true, message: '请输入用户名' },
                { min: 3, max: 50, message: '用户名为 3-50 个字符' },
                { pattern: /^[a-zA-Z0-9_]+$/, message: '仅支持字母、数字和下划线' },
              ]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="例如 demo_user"
                autoComplete="username"
              />
            </Form.Item>
            <Form.Item
              name="nickname"
              label="昵称（可选）"
              rules={[{ max: 50, message: '昵称不能超过 50 个字符' }]}
            >
              <Input placeholder="怎么称呼你" autoComplete="nickname" />
            </Form.Item>
          </div>

          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' },
              { max: 100, message: '邮箱不能超过 100 个字符' },
            ]}
          >
            <Input prefix={<MailOutlined />} placeholder="you@example.com" autoComplete="email" />
          </Form.Item>
        </>
      )}

      <Form.Item
        name="password"
        label="密码"
        rules={[
          { required: true, message: '请输入密码' },
          ...(mode === 'register'
            ? [{ min: 6, max: 64, message: '密码为 6-64 个字符' }]
            : []),
        ]}
      >
        <Input.Password
          prefix={<LockOutlined />}
          placeholder={mode === 'login' ? '请输入密码' : '至少 6 位字符'}
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
        />
      </Form.Item>

      {mode === 'register' ? (
        <Form.Item
          name="confirmPassword"
          label="确认密码"
          dependencies={['password']}
          rules={[
            { required: true, message: '请再次输入密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('password') === value) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error('两次输入的密码不一致'));
              },
            }),
          ]}
        >
          <Input.Password
            prefix={<LockOutlined />}
            placeholder="再次输入密码"
            autoComplete="new-password"
          />
        </Form.Item>
      ) : null}

      <Button
        type="primary"
        htmlType="submit"
        block
        loading={submitting}
        className="auth-submit"
      >
        {mode === 'login' ? '登录并继续' : '注册并继续'}
      </Button>
    </Form>
  );
}
