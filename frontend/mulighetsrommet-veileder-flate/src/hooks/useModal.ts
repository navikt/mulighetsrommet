import { useState } from 'react';

export const useModal = () => {
  const [isOpen, setIsOpen] = useState(false);

  function toggle(state: boolean) {
    setIsOpen(state);
  }

  return {
    isOpen,
    toggle,
  };
};
