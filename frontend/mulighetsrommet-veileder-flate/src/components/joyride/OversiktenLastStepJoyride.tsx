import Joyride, { CallBackProps, EVENTS, STATUS } from 'react-joyride';
import { localeStrings } from './utils';
import { stepsLastStep } from './Steps';
import { useAtom } from 'jotai';
import { joyrideAtom } from '../../core/atoms/atoms';

export const OversiktenLastStepJoyride = () => {
  const [joyride, setJoyride] = useAtom(joyrideAtom);

  const handleJoyrideCallbackLastStep = (data: CallBackProps) => {
    const { status, type } = data;
    if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status) || EVENTS.TOOLTIP_CLOSE === type) {
      setJoyride({ ...joyride, joyrideOversiktenLastStep: false });
    }
  };

  return (
    <Joyride
      locale={localeStrings()}
      run={joyride.joyrideOversiktenLastStep === true}
      steps={stepsLastStep}
      callback={handleJoyrideCallbackLastStep}
      disableScrolling
    />
  );
};
