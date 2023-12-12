import { Avtale } from "mulighetsrommet-api-client";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import styles from "../DetaljerInfo.module.scss";
import { useAvtaleEndringshistorikk } from "../../api/avtaler/useAvtaleEndringshistorikk";
import { ViewEndringshistorikk } from "../../components/endringshistorikk/ViewEndringshistorikk";
import { EndringshistorikkPopover } from "../../components/endringshistorikk/EndringshistorikkPopover";

interface Props {
  avtale: Avtale;
}

export function AvtaleKnapperad({ avtale }: Props) {
  return (
    <div className={styles.knapperad}>
      <EndringshistorikkPopover>
        <AvtaleEndringshistorikk id={avtale.id} />
      </EndringshistorikkPopover>

      <Lenkeknapp size="small" to={`/avtaler/${avtale.id}/skjema`} variant="primary">
        Rediger avtale
      </Lenkeknapp>
    </div>
  );
}

function AvtaleEndringshistorikk({ id }: { id: string }) {
  const historikk = useAvtaleEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
