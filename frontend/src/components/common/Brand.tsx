import { Link } from 'react-router-dom';

interface BrandProps {
  to?: string;
  compact?: boolean;
}

export default function Brand({ to = '/', compact = false }: BrandProps) {
  return (
    <Link className={`brand${compact ? ' is-compact' : ''}`} to={to} aria-label="AI Video Studio">
      <span className="brand-mark" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M7 4.5h10a2.5 2.5 0 0 1 2.5 2.5v10a2.5 2.5 0 0 1-2.5 2.5H7A2.5 2.5 0 0 1 4.5 17V7A2.5 2.5 0 0 1 7 4.5Z"
            stroke="currentColor"
            strokeWidth="1.4"
          />
          <path d="M9 8.2h6M9 11.7h3.8M9 15.2h5.4" stroke="currentColor" strokeWidth="1.4" />
          <path d="m16.3 13.1 2.2 1.6-2.2 1.5v-3.1Z" fill="currentColor" />
        </svg>
      </span>
      <span className="brand-name">
        AI Video <em>Studio</em>
      </span>
      {!compact ? <span className="brand-tag">NOCTURNE</span> : null}
    </Link>
  );
}
