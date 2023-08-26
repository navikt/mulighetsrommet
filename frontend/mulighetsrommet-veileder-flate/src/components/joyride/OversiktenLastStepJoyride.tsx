import Joyride, { ACTIONS, CallBackProps, EVENTS, STATUS } from 'react-joyride';
import { stepsLastStep } from './Steps';
import { locale, styling } from './config';
import { useAtom } from 'jotai';
import { joyrideAtom } from '../../core/atoms/atoms';

export const OversiktenLastStepJoyride = () => {
  const [joyride, setJoyride] = useAtom(joyrideAtom);

  const handleJoyrideCallbackLastStep = (data: CallBackProps) => {
    const { status, type, action } = data;
    if (([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status) || EVENTS.TOOLTIP_CLOSE === type) {
      setJoyride({ ...joyride, joyrideOversiktenLastStep: false });
    }

    //lukker joyride ved klikk på escape
    if (ACTIONS.CLOSE === action) {
      setJoyride({ ...joyride, joyrideOversiktenLastStep: false });
    }
  };

  return (
    <Joyride
      locale={locale}
      run={joyride.joyrideOversiktenLastStep === true}
      steps={stepsLastStep}
      callback={handleJoyrideCallbackLastStep}
      disableScrolling
      hideCloseButton
      styles={styling}
      disableCloseOnEsc={false}
      disableOverlayClose={true}
    />
  );
};
