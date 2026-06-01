import { CheckboxGroup } from "@mr/frontend-common";
import { useGetOppgavetyper } from "@/api/oppgaver/useGetOppgavetyper";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
}

export function OppgaveTypeFilter({ value, onChange }: Props) {
  const { data: oppgavetyper } = useGetOppgavetyper();
  return (
    <CheckboxGroup
      legend="Oppgavetyper"
      hideLegend
      value={value}
      items={oppgavetyper.map(({ type, navn }) => ({ id: type, navn }))}
      onChange={onChange}
    />
  );
}
