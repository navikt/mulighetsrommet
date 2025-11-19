import { Heading, VStack } from "@navikt/ds-react";
import { Definisjonsliste, Definisjonsliste2 } from "../common/Definisjonsliste";
import { Separator } from "../common/Separator";
import { DataDetails } from "@api-client";

export function SatsPerioderOgBelop({
  belop,
  satsDetaljer,
}: {
  belop: number;
  satsDetaljer: DataDetails[];
}) {
  return (
    <VStack className="max-w-[400px]" gap="2">
      {satsDetaljer.length > 0 && (
        <>
          {satsDetaljer.map((s) => (
            <VStack>
              {satsDetaljer.length > 1 && <Heading size="xsmall">{s.header}</Heading>}
              <Definisjonsliste2 definitions={s.entries} className="my-2" />
            </VStack>
          ))}
          <Separator />
        </>
      )}
      <Definisjonsliste
        definitions={[
          {
            key: "Beløp",
            value: String(belop),
            format: "NOK",
          },
        ]}
      />
    </VStack>
  );
}
