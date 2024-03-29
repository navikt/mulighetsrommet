import { Avtale, NavAnsatt } from "mulighetsrommet-api-client";
import styles from "../DetaljerInfo.module.scss";
import { useAvtaleEndringshistorikk } from "@/api/avtaler/useAvtaleEndringshistorikk";
import { ViewEndringshistorikk } from "../../components/endringshistorikk/ViewEndringshistorikk";
import { EndringshistorikkPopover } from "../../components/endringshistorikk/EndringshistorikkPopover";
import { RedigeringsAdvarselModal } from "../../components/modal/RedigeringsAdvarselModal";
import { Button } from "@navikt/ds-react";
import { useRef } from "react";
import { useNavigate } from "react-router-dom";
import { HarSkrivetilgang } from "../../components/authActions/HarSkrivetilgang";

interface Props {
  bruker: NavAnsatt;
  avtale: Avtale;
}

export function AvtaleKnapperad({ bruker, avtale }: Props) {
  const navigate = useNavigate();
  const advarselModal = useRef<HTMLDialogElement>(null);

  return (
    <div className={styles.knapperad}>
      <EndringshistorikkPopover>
        <AvtaleEndringshistorikk id={avtale.id} />
      </EndringshistorikkPopover>

      <HarSkrivetilgang ressurs="Avtale">
        <Button
          size="small"
          variant="primary"
          onClick={() => {
            if (
              avtale.administratorer &&
              avtale.administratorer.length > 0 &&
              !avtale.administratorer.map((a) => a.navIdent).includes(bruker.navIdent)
            ) {
              advarselModal.current?.showModal();
            } else {
              navigate("skjema");
            }
          }}
        >
          Rediger
        </Button>
      </HarSkrivetilgang>
      <RedigeringsAdvarselModal
        ressursNavn="avtalen"
        modalRef={advarselModal}
        onRediger={() => navigate("skjema")}
      />
    </div>
  );
}

function AvtaleEndringshistorikk({ id }: { id: string }) {
  const historikk = useAvtaleEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
