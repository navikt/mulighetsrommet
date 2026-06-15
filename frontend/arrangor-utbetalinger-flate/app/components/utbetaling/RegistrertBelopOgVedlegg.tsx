import { VStack } from "@navikt/ds-react";
import { ValutaBelop } from "@arrangor-utbetalinger/api-client";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { VedleggSummary } from "~/components/utbetaling/VedleggSummary";

interface RegistrertBelopOgVedleggProps {
  belop: ValutaBelop;
  vedlegg: File[];
}

export function RegistrertBelopOgVedlegg({ belop, vedlegg }: RegistrertBelopOgVedleggProps) {
  return (
    <VStack gap="space-4">
      <Definisjonsliste definitions={[{ key: "Beløp", value: formaterValutaBelop(belop) }]} />
      <VedleggSummary vedlegg={vedlegg} />
    </VStack>
  );
}
