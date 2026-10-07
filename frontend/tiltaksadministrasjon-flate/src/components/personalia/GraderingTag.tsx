import { Tag } from "@navikt/ds-react";
import { Gradering } from "@tiltaksadministrasjon/api-client";
import { graderingVisning } from "./graderingVisning";

const TAG_GRADERINGER: Gradering[] = [
  Gradering.STRENGT_FORTROLIG_ADRESSE,
  Gradering.STRENGT_FORTROLIG_UTLAND,
  Gradering.FORTROLIG_ADRESSE,
  Gradering.SKJERMING,
];

interface Props {
  gradering: Gradering;
}

export function GraderingTag({ gradering }: Props) {
  const visning = graderingVisning(gradering);
  if (!visning || !TAG_GRADERINGER.includes(gradering)) {
    return null;
  }

  const { Ikon, label, color } = visning;
  return (
    <Tag size="small" variant="moderate" data-color={color} icon={<Ikon aria-hidden />}>
      {label}
    </Tag>
  );
}
