import { Button, Result } from 'antd';
import { useNavigate } from 'react-router-dom';

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="not-found-page">
      <Result
        status="404"
        title="404"
        subTitle="这个页面不在片场里。"
        extra={
          <Button type="primary" onClick={() => navigate('/create')}>
            返回创作台
          </Button>
        }
      />
    </div>
  );
}
