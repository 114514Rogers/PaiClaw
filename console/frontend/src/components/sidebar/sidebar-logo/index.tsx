import { ReactElement } from 'react';
import { useNavigate } from 'react-router-dom';

const SidebarLogo = (): ReactElement => {
  const navigate = useNavigate();

  const handleLogoClick = (): void => {
    navigate('/');
  };

  return (
    <div
      className="flex cursor-pointer items-center justify-center gap-2"
      onClick={handleLogoClick}
    >
      <img
        src="/paiflow-icon.svg"
        className="h-10 w-10 flex-shrink-0"
        alt="PaiFlow"
      />
      <span
        className="text-[30px] font-semibold leading-none"
        style={{ color: '#334155' }}
      >
        PaiFlow
      </span>
    </div>
  );
};

export default SidebarLogo;
