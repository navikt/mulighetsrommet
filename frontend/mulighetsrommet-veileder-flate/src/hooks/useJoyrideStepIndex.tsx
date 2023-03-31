import { useState } from 'react';

export const useJoyrideStepIndex = () => {
  const [stepIndexState, setState] = useState(0);

  function setStepIndexState(index: number | ((prev: number) => number)) {
    setState(index);
  }

  return {
    stepIndexState,
    setStepIndexState,
  };
};
