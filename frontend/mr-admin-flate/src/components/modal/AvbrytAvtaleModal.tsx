import { AvbrytAvtaleAarsak, Avtale } from "mulighetsrommet-api-client";
import { RefObject, useEffect, useState } from "react";
import { useAvbrytAvtale } from "@/api/avtaler/useAvbrytAvtale";
import { useNavigate } from "react-router-dom";
import { useAktiveTiltaksgjennomforingerByAvtaleId } from "@/api/tiltaksgjennomforing/useAktiveTiltaksgjennomforingerByAvtaleId";
import { VarselModal } from "@/components/modal/VarselModal";
import { Alert, BodyShort, Button, Radio } from "@navikt/ds-react";
import { AvbrytModalError } from "@/components/modal/AvbrytModalError";
import { AvbrytModalAarsaker } from "@/components/modal/AvbrytModalAarsaker";
import { avbrytAvtaleAarsakToString } from "@/utils/Utils";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import style from "./AvbrytGjennomforingAvtaleModal.module.scss";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  avtale: Avtale;
}

export function AvbrytAvtaleModal({ modalRef, avtale }: Props) {
  const mutation = useAvbrytAvtale();
  const navigate = useNavigate();
  const { data: tiltaksgjennomforingerMedAvtaleId } = useAktiveTiltaksgjennomforingerByAvtaleId(
    avtale.id,
  );

  const [aarsak, setAarsak] = useState<string | null>(null);
  const [customAarsak, setCustomAarsak] = useState<string | null>(null);
  const avtalenHarGjennomforinger =
    tiltaksgjennomforingerMedAvtaleId && tiltaksgjennomforingerMedAvtaleId.data.length > 0;

  const onClose = () => {
    mutation.reset();
    modalRef.current?.close();
  };

  useEffect(() => {
    modalRef.current?.close();
    navigate(`/avtaler/${avtale.id}`);
  }, [mutation.isSuccess]);

  const handleAvbrytAvtale = () => {
    mutation.reset();
    if (avtale?.id) {
      mutation.mutate({
        id: avtale?.id,
        aarsak: aarsak === "annet" ? customAarsak : aarsak,
      });
    }
  };

  function pluralGjennomforingTekst(antall: number, tekst: string) {
    return tiltaksgjennomforingerMedAvtaleId &&
      tiltaksgjennomforingerMedAvtaleId.data.length > antall
      ? tekst
      : "";
  }

  return (
    <VarselModal
      modalRef={modalRef}
      handleClose={onClose}
      headingIconType="error"
      headingText={
        mutation.isError || avtalenHarGjennomforinger
          ? `Kan ikke avbryte «${avtale?.navn}»`
          : `Ønsker du å avbryte «${avtale?.navn}»?`
      }
      body={
        <>
          {avtalenHarGjennomforinger ? (
            <Alert variant="warning">
              {`Avtaler med aktive gjennomføringer kan ikke avbrytes. Det er 
                ${tiltaksgjennomforingerMedAvtaleId.data.length} 
                aktiv${pluralGjennomforingTekst(1, "e")} 
                gjennomføring${pluralGjennomforingTekst(1, "er")} 
                under denne avtalen. Vurder om du vil avbryte 
                gjennomføringen${pluralGjennomforingTekst(0, "e")}. `}
            </Alert>
          ) : (
            <AvbrytModalAarsaker
              aarsak={aarsak}
              customAarsak={customAarsak}
              setAarsak={setAarsak}
              setCustomAarsak={setCustomAarsak}
              mutation={mutation}
              radioknapp={
                <>
                  {(Object.keys(AvbrytAvtaleAarsak) as Array<AvbrytAvtaleAarsak>)
                    .filter((a) => a !== AvbrytAvtaleAarsak.AVBRUTT_I_ARENA)
                    .map((a) => (
                      <Radio key={`${a}`} value={a}>
                        {avbrytAvtaleAarsakToString(a)}
                      </Radio>
                    ))}
                </>
              }
            />
          )}

          {mutation?.isError && (
            <BodyShort>
              <AvbrytModalError aarsak={aarsak} customAarsak={customAarsak} mutation={mutation} />
            </BodyShort>
          )}
        </>
      }
      secondaryButton={!avtalenHarGjennomforinger}
      primaryButton={
        avtalenHarGjennomforinger ? (
          <Button onClick={onClose}>Ok</Button>
        ) : (
          <HarSkrivetilgang ressurs="Avtale">
            <Button variant="danger" onClick={handleAvbrytAvtale}>
              Ja, jeg vil avbryte avtalen
            </Button>
          </HarSkrivetilgang>
        )
      }
      footerClassName={avtalenHarGjennomforinger ? style.footer_ok : ""}
    />
  );
}
