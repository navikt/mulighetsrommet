import { useMigrerteTiltakstyper } from "@/api/tiltakstyper/useMigrerteTiltakstyper";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { BodyShort, Heading, Modal, VStack } from "@navikt/ds-react";
import { RedaksjoneltInnholdModalBody } from "@/components/modal/RedaksjoneltInnholdModalBody";
import { RedaksjoneltInnholdModalContainer } from "@/components/modal/RedaksjoneltInnholdModalContainer";

interface OpprettTiltakIArenaModalProps {
  open: boolean;
  onClose: () => void;
  tiltakstype: string;
}

export function OpprettTiltakIArenaModal({
  open,
  onClose,
  tiltakstype,
}: OpprettTiltakIArenaModalProps) {
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyper();
  const { data: tiltakstyper } = useTiltakstyper();

  const migrerteTiltakstyperNavn =
    tiltakstyper?.data
      .filter(
        (tiltakstype) =>
          tiltakstype.tiltakskode && migrerteTiltakstyper?.includes(tiltakstype.tiltakskode),
      )
      .map((tiltakstype) => tiltakstype.navn) ?? [];

  return (
    <RedaksjoneltInnholdModalContainer modalOpen={open} onClose={onClose}>
      <Modal.Header closeButton>
        <Heading size="medium">Tiltaksgjennomføring kan ikke opprettes her</Heading>
      </Modal.Header>
      <RedaksjoneltInnholdModalBody>
        <VStack gap="2">
          <BodyShort>
            Tiltak knyttet til tiltakstypen <code>{tiltakstype}</code> kan ikke opprettes her enda.
            Du må fortsatt opprette tiltaksgjennomføringer for denne tiltakstypen i Arena.
          </BodyShort>
          {migrerteTiltakstyperNavn.length > 0 ? (
            <>
              <Heading size="small" level="4">
                Du kan opprette tiltak for følgende tiltakstyper her i Nav Tiltaksadministrasjon:
              </Heading>
              <ul>
                {migrerteTiltakstyperNavn.map((tiltakstype) => (
                  <li key={tiltakstype}>{tiltakstype}</li>
                ))}
              </ul>
            </>
          ) : null}
        </VStack>
      </RedaksjoneltInnholdModalBody>
    </RedaksjoneltInnholdModalContainer>
  );
}
