import { useAvtale } from "@/api/avtaler/useAvtale";
import {
  useGjennomforing,
  useGjennomforingByPathParam,
} from "@/api/gjennomforing/useGjennomforing";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
import { useUpdateGjennomforingVeilederinformasjon } from "@/api/gjennomforing/useUpdateGjennomforingVeilederinformasjon";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { defaultGjennomforingData } from "@/components/gjennomforing/GjennomforingFormConst";
import { GjennomforingInformasjonForVeiledereForm } from "@/components/gjennomforing/GjennomforingInformasjonForVeiledereForm";
import { useEditForm } from "@/hooks/useEditForm";
import { gjennomforingVeilederinfoSchema } from "@/schemas/gjennomforing";
import {
  GjennomforingAvtaleDto,
  GjennomforingVeilederinfoRequest,
} from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { RedigerGjennomforingPageLayout } from "@/pages/gjennomforing/RedigerGjennomforingPageLayout";
import {
  GjennomforingFormValues,
  toGjennomforingVeilederinfoRequest,
} from "./gjennomforingFormUtils";

export function RedigerGjennomforingVeilederinformasjonPage() {
  const detaljer = useGjennomforingByPathParam();

  if (!isGruppetiltak(detaljer.gjennomforing)) {
    return null;
  }

  return (
    <RedigerVeilederinformasjonForm
      gjennomforingId={detaljer.gjennomforing.id}
      gjennomforing={detaljer.gjennomforing}
    />
  );
}

interface FormProps {
  gjennomforingId: string;
  gjennomforing: GjennomforingAvtaleDto;
}

function RedigerVeilederinformasjonForm({ gjennomforingId, gjennomforing }: FormProps) {
  const navigate = useNavigate();
  const detaljer = useGjennomforing(gjennomforingId);
  const { data: avtale } = useAvtale(gjennomforing.avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);

  const { methods, onSubmit } = useEditForm<
    GjennomforingFormValues,
    GjennomforingVeilederinfoRequest
  >({
    schema: gjennomforingVeilederinfoSchema,
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
    mutation: useUpdateGjennomforingVeilederinformasjon(gjennomforingId),
    toRequest: toGjennomforingVeilederinfoRequest,
    onSuccess: () => navigate(`/gjennomforinger/${gjennomforingId}/redaksjonelt-innhold`),
  });

  return (
    <RedigerGjennomforingPageLayout
      seksjonsnavn="informasjon for veiledere"
      methods={methods}
      onSubmit={onSubmit}
    >
      <GjennomforingInformasjonForVeiledereForm
        avtale={avtale}
        veilederinfo={detaljer.veilederinfo}
      />
    </RedigerGjennomforingPageLayout>
  );
}
