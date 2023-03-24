import Joyride, { Step } from 'react-joyride';

const steps: Step[] = [
  {
    target: '#historikkBtn',
    content: 'Her kan du hvilke tiltak brukeren har vært på tidligere',
    placement: 'auto',
    disableBeacon: false,
  },
];

export function VeilederJoyride() {
  return (
    <Joyride
      locale={{
        close: 'Lukk',
      }}
      run={true}
      steps={steps}
    />
  );
}
