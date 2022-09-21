import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import CheckboxFilter from './CheckboxFilter';

const typer = [
  {
    id: 'individuelt',
    tittel: 'Individuelle tiltak',
  },
  {
    id: 'gruppe',
    tittel: 'Gruppetiltak',
  },
];

export function FilterForIndividueltEllerGruppetiltak() {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  return (
    <CheckboxFilter
      accordionNavn="Gruppe eller individuelle tiltak"
      options={filter.typeTiltak}
      setOptions={typeTiltak => setFilter({ ...filter, typeTiltak })}
      data={
        typer.map(({ id, tittel }) => {
          return {
            id,
            tittel,
          };
        }) ?? []
      }
      isLoading={false}
      isError={false}
      sortert
      defaultOpen={filter.typeTiltak.length > 0}
    />
  );
}
