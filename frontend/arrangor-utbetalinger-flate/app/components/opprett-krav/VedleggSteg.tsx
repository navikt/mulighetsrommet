import {
  FieldError,
  OpprettKravVedleggSteg,
  OpprettKravVedleggStegGuidePanelType,
} from "@arrangor-utbetalinger/api-client";
import { FileObject, GuidePanel, Heading, VStack } from "@navikt/ds-react";
import { OpprettKravFormState } from "~/routes/$orgnr.opprett-krav.$gjennomforingid";
import { VedleggUpload } from "~/components/utbetaling/VedleggUpload";
import { errorAt } from "~/utils/validering";

interface VedleggStepProps {
  data: OpprettKravVedleggSteg;
  formState: OpprettKravFormState;
  updateFormState: (updates: Partial<OpprettKravFormState>) => void;
  errors: FieldError[];
}

export default function VedleggSteg({
  data,
  formState,
  updateFormState,
  errors,
}: VedleggStepProps) {
  return (
    <>
      <Heading level="2" spacing size="large">
        Vedlegg
      </Heading>
      <VStack gap="space-24">
        <GuidePanelVedlegg type={data.guidePanel} />
        <VedleggUpload
          files={formState.files}
          onFilesChange={(files: FileObject[]) => updateFormState({ files })}
          description="Du kan laste opp PDF-filer. Maks 10 filer. Maks størrelse 10 MB per fil."
          error={errorAt("/vedlegg", errors)}
        />
      </VStack>
    </>
  );
}

interface GuidePanelVedleggProps {
  type: OpprettKravVedleggStegGuidePanelType | null;
}

function GuidePanelVedlegg({ type }: GuidePanelVedleggProps) {
  switch (type) {
    case OpprettKravVedleggStegGuidePanelType.INVESTERING_VTA_AFT:
      return (
        <GuidePanel>
          Du må laste opp vedlegg som dokumenterer de faktiske kostnadene dere har hatt for
          investeringer
        </GuidePanel>
      );
    case OpprettKravVedleggStegGuidePanelType.AVTALT_PRIS:
      return (
        <GuidePanel>
          Her skal du laste opp faktura og eventuelt andre vedlegg som er relevante for utbetalingen
        </GuidePanel>
      );
    case null:
      return null;
  }
}
