import { BodyShort, Button, Heading } from '@navikt/ds-react';
import { WarningColored } from '@navikt/ds-icons';
import delemodalStyles from './Delemodal.module.scss';

interface KanIkkeDeleMedBrukerModalProps {
  setModalOpen: () => void;
  manuellOppfolging: boolean;
  krrStatusErReservert: boolean;
  manuellStatus: boolean;
  kanDeleMedBruker: boolean;
}

export const KanIkkeDeleMedBrukerModal = ({
  setModalOpen,
  manuellOppfolging,
  krrStatusErReservert,
  manuellStatus,
  kanDeleMedBruker,
}: KanIkkeDeleMedBrukerModalProps) => {
  const feilmelding = () => {
    if (manuellOppfolging)
      return 'Brukeren får manuell oppfølging og kan ikke benytte seg av de digitale tjenestene våre.';
    else if (krrStatusErReservert)
      return 'Brukeren har reservert seg mot elektronisk kommunikasjon i Kontakt- og reservasjonsregisteret (KRR).';
    else if (manuellStatus)
      return 'Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert seg mot elektronisk kommunikasjon.';
    else if (!kanDeleMedBruker)
      return 'Brukeren får manuell oppfølging og kan derfor ikke benytte seg av de digitale tjenestene våre. Brukeren har også reservert seg mot elektronisk kommunikasjon i Kontakt- og reservasjonsregisteret (KRR).';
    else return 'Det har oppstått en feil. Vennligst prøv igjen senere.';
  };

  return (
    <>
      <WarningColored className={delemodalStyles.delemodal_svg} />
      <Heading level="1" size="large" className={delemodalStyles.feilmelding_heading}>
        Kunne ikke dele tiltaket
      </Heading>
      <BodyShort className={delemodalStyles.feilmelding_text}>{feilmelding()}</BodyShort>
      <Button variant="primary" className={delemodalStyles.feilmelding_btn} onClick={() => setModalOpen()}>
        OK
      </Button>
    </>
  );
};
