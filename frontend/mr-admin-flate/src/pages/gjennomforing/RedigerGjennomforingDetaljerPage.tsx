import { useAvtale } from "@/api/avtaler/useAvtale";
import {
  useGjennomforing,
  useGjennomforingByPathParam,
} from "@/api/gjennomforing/useGjennomforing";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
import { useUpdateGjennomforingDetaljer } from "@/api/gjennomforing/useUpdateGjennomforingDetaljer";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { defaultGjennomforingData } from "@/components/gjennomforing/GjennomforingFormConst";
import { GjennomforingFormDetaljer } from "@/components/gjennomforing/GjennomforingFormDetaljer";
import { useEditForm } from "@/hooks/useEditForm";
import { gjennomforingDetaljerSchema } from "@/schemas/gjennomforing";
import {
  GjennomforingAvtaleDto,
  GjennomforingDetaljerRequest,
} from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { RedigerGjennomforingPageLayout } from "./RedigerGjennomforingPageLayout";
import { GjennomforingFormValues, toGjennomforingDetaljerRequest } from "./gjennomforingFormUtils";

export function RedigerGjennomforingDetaljerPage() {
  const detaljer = useGjennomforingByPathParam();

  if (!isGruppetiltak(detaljer.gjennomforing)) {
    return null;
  }

  return (
    <RedigerDetaljerForm
      gjennomforingId={detaljer.gjennomforing.id}
      gjennomforing={detaljer.gjennomforing}
    />
  );
}

interface FormProps {
  gjennomforingId: string;
  gjennomforing: GjennomforingAvtaleDto;
}

function RedigerDetaljerForm({ gjennomforingId, gjennomforing }: FormProps) {
  const navigate = useNavigate();
  const detaljer = useGjennomforing(gjennomforingId);
  const { data: avtale } = useAvtale(gjennomforing.avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);

  const { methods, onSubmit } = useEditForm<GjennomforingFormValues, GjennomforingDetaljerRequest>({
    schema: gjennomforingDetaljerSchema,
    defaultValues: defaultGjennomforingData(
      ansatt,
      tiltakstype,
      avtale,
      gjennomforing,
      detaljer.veilederinfo,
      detaljer.prismodell,
      detaljer.amoKategorisering,
      detaljer.utdanningslop,
    ),
    mutation: useUpdateGjennomforingDetaljer(gjennomforingId),
    toRequest: toGjennomforingDetaljerRequest,
    onSuccess: () => navigate(`/gjennomforinger/${gjennomforingId}`),
  });

  return (
    <RedigerGjennomforingPageLayout seksjonsnavn="detaljer" methods={methods} onSubmit={onSubmit}>
      <GjennomforingFormDetaljer
        tiltakstype={tiltakstype}
        avtale={avtale}
        gjennomforing={gjennomforing}
        veilederinfo={detaljer.veilederinfo}
        deltakere={null}
      />
    </RedigerGjennomforingPageLayout>
  );
}
