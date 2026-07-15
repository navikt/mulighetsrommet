import { CheckboxDropdownGroup } from "@mr/frontend-common";
import { useGetOppgavetyper } from "@/api/oppgaver/useGetOppgavetyper";
import { OppgaveTypeStruktur } from "@tiltaksadministrasjon/api-client";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
}

export function OppgaveTypeFilter({ value, onChange }: Props) {
  const { data: oppgavetyper } = useGetOppgavetyper();
  const groups = toCheckboxGroups(oppgavetyper);
  return (
    <CheckboxDropdownGroup legend="Oppgavetyper" value={value} items={groups} onChange={onChange} />
  );
}

function toCheckboxGroups(struktur: OppgaveTypeStruktur[]) {
  return struktur.map(({ kategori, typer }) => {
    return {
      id: kategori,
      navn: kategori,
      items: typer.map((type) => ({
        id: type.type,
        navn: type.navn,
        erStandardvalg: true,
      })),
    };
  });
}
