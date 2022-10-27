import { useFeatureToggles } from '../../core/api/feature-toggles';
import ViewTiltakstypeDetaljer from '../tiltaksgjennomforing-detaljer/ViewTiltakstypeDetaljer';

export function SanityPreview() {
  const features = useFeatureToggles();
  if (features.data && !features.data['mulighetsrommet.enable-previewflate']) {
    return <p>Preview-flate er ikke aktivert enda</p>;
  }

  return (
    <div>
      <div style={{ margin: '0 1rem', background: '#f07575', padding: '5px' }}>
        <h1>Forhåndsvisning av informasjon fra Sanity</h1>
        <p>Her skal det på sikt dukke opp forhåndsvisning av informasjon fra Sanity...</p>
      </div>
      <ViewTiltakstypeDetaljer />
    </div>
  );
}
