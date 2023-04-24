import { Tabs } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { useFeatureToggles, VIS_INNSIKTSFANE } from '../../core/api/feature-toggles';
import { logEvent } from '../../core/api/logger';
import useTiltaksgjennomforingById from '../../core/api/queries/useTiltaksgjennomforingById';
import { faneAtom } from '../../core/atoms/atoms';
import { kebabCase } from '../../utils/Utils';
import DetaljerFane from './DetaljerFane';
import KontaktinfoFane from './kontaktinfofane/KontaktinfoFane';
import styles from './TiltaksdetaljerFane.module.scss';

const TiltaksdetaljerFane = () => {
  const { data } = useTiltaksgjennomforingById();
  const [fane, setFane] = useAtom(faneAtom);
  const features = useFeatureToggles();
  const visInnsiktsfane = features.isSuccess && features.data[VIS_INNSIKTSFANE];

  if (!data) return null;

  const { tiltakstype, faneinnhold } = data;
  const faneoverskrifter = [
    'For hvem',
    'Detaljer og innhold',
    'Påmelding og varighet',
    'Kontaktinfo',
    'Innsikt',
  ] as const;
  const tabValueTilFaneoverSkrifter: { [key: string]: string } = {
    tab1: faneoverskrifter[0],
    tab2: faneoverskrifter[1],
    tab3: faneoverskrifter[2],
    tab4: faneoverskrifter[3],
    tab5: faneoverskrifter[4],
  };

  return (
    <Tabs
      defaultValue={fane}
      size="small"
      selectionFollowsFocus
      className={styles.fane_root}
      onChange={value => {
        logEvent('mulighetsrommet.faner', { value: tabValueTilFaneoverSkrifter[value] });
        setFane(value);
      }}
    >
      <Tabs.List className={styles.fane_liste} id="fane_liste">
        {faneoverskrifter
          .filter(fane => {
            return !(!visInnsiktsfane && fane === 'Innsikt');
          })
          .map((fane, index) => (
            <Tabs.Tab
              key={index}
              value={`tab${index + 1}`}
              label={fane}
              className={styles.btn_tab}
              data-testid={`fane_${kebabCase(fane)}`}
            />
          ))}
      </Tabs.List>
      <div className={styles.fane_panel}>
        <Tabs.Panel value="tab1" data-testid="tab1">
          <DetaljerFane
            tiltaksgjennomforingAlert={faneinnhold?.forHvemInfoboks}
            tiltakstypeAlert={tiltakstype.faneinnhold?.forHvemInfoboks}
            tiltaksgjennomforing={faneinnhold?.forHvem}
            tiltakstype={tiltakstype.faneinnhold?.forHvem}
          />
        </Tabs.Panel>
        <Tabs.Panel value="tab2" data-testid="tab2">
          <DetaljerFane
            tiltaksgjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
            tiltakstypeAlert={tiltakstype.faneinnhold?.detaljerOgInnholdInfoboks}
            tiltaksgjennomforing={faneinnhold?.detaljerOgInnhold}
            tiltakstype={tiltakstype.faneinnhold?.detaljerOgInnhold}
          />
        </Tabs.Panel>
        <Tabs.Panel value="tab3" data-testid="tab3">
          <DetaljerFane
            tiltaksgjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
            tiltakstypeAlert={tiltakstype.faneinnhold?.pameldingOgVarighetInfoboks}
            tiltaksgjennomforing={faneinnhold?.pameldingOgVarighet}
            tiltakstype={tiltakstype.faneinnhold?.pameldingOgVarighet}
          />
        </Tabs.Panel>
        <Tabs.Panel value="tab4" data-testid="tab4">
          <KontaktinfoFane />
        </Tabs.Panel>
      </div>
    </Tabs>
  );
};

export default TiltaksdetaljerFane;
