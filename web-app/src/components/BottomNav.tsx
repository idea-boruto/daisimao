import { NavLink, useLocation } from 'react-router-dom';

const tabs = [
  { to: '/', label: '任务大厅', icon: '📋' },
  { to: '/publish', label: '发布', icon: '✏️' },
  { to: '/my-tasks', label: '我的任务', icon: '📦' },
  { to: '/profile', label: '我的', icon: '👤' },
];

export default function BottomNav() {
  const location = useLocation();
  const isActive = (to: string) => location.pathname === to || (to !== '/' && location.pathname.startsWith(to));

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 z-50 safe-area-bottom">
      <div className="max-w-lg mx-auto flex">
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={`flex-1 flex flex-col items-center py-1.5 text-xs transition-colors ${
              isActive(tab.to) ? 'text-primary' : 'text-gray-500'
            }`}
          >
            <span className="text-xl mb-0.5">{tab.icon}</span>
            {tab.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
