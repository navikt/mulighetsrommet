import Joyride, { EVENTS, STATUS } from 'react-joyride';
import { localeStrings } from './utils';
import { stepsLastStep } from './Steps';
import { useAtom } from 'jotai';
import { joyrideAtom } from '../../core/atoms/atoms';

export const OversiktenLastStepJoyride = () => {
  const [joyride, setJoyride] = useAtom(joyrideAtom);

  const handleJoyrideCallbackLastStep = (data: any) => {
    const { status, type } = data;
    if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status) || [EVENTS.TOOLTIP_CLOSE].includes(type)) {
      setJoyride({ ...joyride, joyrideOversiktenLastStep: false });
    }
  };

  return (
    <Joyride
      locale={localeStrings()}
      run={joyride.joyrideOversiktenLastStep === true}
      steps={stepsLastStep}
      callback={handleJoyrideCallbackLastStep}
    />
  );
};
