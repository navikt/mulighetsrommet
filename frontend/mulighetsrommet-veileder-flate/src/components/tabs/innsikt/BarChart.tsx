import { BarStackHorizontal } from '@visx/shape';
import { Group } from '@visx/group';
import { AxisBottom, AxisLeft } from '@visx/axis';
import { GridColumns } from '@visx/grid';
import { scaleBand, scaleLinear, scaleOrdinal } from '@visx/scale';
import { LegendOrdinal } from '@visx/legend';
import { Datapunkt } from './Datapunkt';
import useHentStatistikkFraFil from '../../../hooks/useHentStatistikkFraFil';
import '../TiltaksdetaljerFane.less';
import React, { useState } from 'react';
import { StatistikkFraCsvFil } from '../../../api/models';

type Status = 'Arbeidstaker m. ytelse/oppf' | 'Kun arbeidstaker' | 'Registrert hos Nav' | 'Ukjent';

function isOfStatusType(value: any): value is Status {
  return ['Arbeidstaker m. ytelse/oppf', 'Kun arbeidstaker', 'Registrert hos Nav', 'Ukjent'].includes(value);
}

function csvObjectArrayTilDatapunktArray(array: StatistikkFraCsvFil[]): Datapunkt[] {
  return array
    .sort((item1, item2) => parseInt(item2['Antall Måneder']) - parseInt(item1['Antall Måneder']))
    .map(item => {
      return {
        År: item['År'],
        tiltakstype: item.Tiltakstype,
        'Arbeidstaker m. ytelse/oppf': parseFloat(replaceCommaWithPeriod(item['Arbeidstaker m. ytelse/oppf'])),
        'Kun arbeidstaker': parseFloat(replaceCommaWithPeriod(item['Kun arbeidstaker'])),
        'Registrert hos Nav': parseFloat(replaceCommaWithPeriod(item['Registrert hos Nav'])),
        Ukjent: parseFloat(replaceCommaWithPeriod(item.Ukjent)),
        antallManeder: item['Antall Måneder'] + ' mnd',
      };
    });
}

/**
 * Erstatter komma med punktum siden tall fra norsk Excel eksporterer tall med komma istedenfor punktum.
 * parseFloat derimot, forventer punktum i flyttall.
 * @param value verdi med komma i seg
 * @returns verdi med punktum som har erstattet komma
 */
function replaceCommaWithPeriod(value: string): string {
  return value.replace(',', '.');
}

export type BarStackHorizontalProps = {
  tiltakstype: string;
  width: number;
  height: number;
  margin?: { top: number; right: number; bottom: number; left: number };
  events?: boolean;
};

const bla = '#748CB2';
const gronn = '#9CC677';
const gul = '#EACF5E';
const rod = '#F9AD79';
const background = '#F1F1F1';
const black = '#000000';
const grey = '#8F8F8F';
const defaultMargin = { top: 20, left: 50, right: 40, bottom: 100 };

// accessors
const getAntallManeder = (d: Datapunkt) => d.antallManeder;

export default function BarChart({ tiltakstype, width, height, margin = defaultMargin }: BarStackHorizontalProps) {
  const csvData = useHentStatistikkFraFil();
  const [chosenYear, setChosenYear] = useState(new Date().getFullYear().toString());

  if (!csvData || csvData.length === 0) {
    console.log('Klarte ikke hente csvData :(');
    return null;
  }
  const data = csvObjectArrayTilDatapunktArray(csvData);
  const allYears = new Set(data.map(it => it.År).sort());

  const filteredData = data.filter(item => item.tiltakstype === tiltakstype && item['År'] === chosenYear);
  if (!data || data.length === 0) {
    return null;
  }

  //prep data
  const keys = Object.keys(filteredData[0]).filter(header => isOfStatusType(header)) as Status[];

  // scales
  const percentageScale = scaleLinear<number>({
    domain: [0, 100],
    nice: false,
  });

  const monthScale = scaleBand<string>({
    domain: filteredData.map(getAntallManeder),
    padding: 0.8,
  });
  const colorScale = scaleOrdinal<Status, string>({
    domain: keys,
    range: [bla, gronn, gul, rod],
  });

  // bounds
  const xMax = width - margin.left - margin.right;
  const yMax = height - margin.top - margin.bottom;

  percentageScale.rangeRound([0, xMax]);
  monthScale.rangeRound([yMax, 0]);

  return width < 10 ? null : (
    <div>
      <div style={{ width }} className={'tiltaksdetaljer__innsiktheader'}>
        Status etter avgang{' '}
        <select
          defaultValue={chosenYear}
          style={{ display: 'inline', border: 'none', borderBottom: '1px solid black' }}
          onChange={e => setChosenYear(e.currentTarget.value)}
          name="aar"
          id="aar"
        >
          {[...allYears].map(year => (
            <option key={year} value={year}>
              {year}
            </option>
          ))}
        </select>
      </div>

      <svg width={width} height={height}>
        <rect width={width} height={height} fill={background} rx={14} />
        <GridColumns
          top={margin.top}
          left={margin.left}
          scale={percentageScale}
          width={xMax}
          height={yMax}
          stroke={grey}
        />
        <Group top={margin.top} left={margin.left}>
          <BarStackHorizontal<Datapunkt, Status>
            data={filteredData}
            keys={keys}
            height={yMax}
            y={getAntallManeder}
            xScale={percentageScale}
            yScale={monthScale}
            color={colorScale}
          >
            {barStacks =>
              barStacks.map(barStack =>
                barStack.bars.map(bar => (
                  <rect
                    key={`barstack-horizontal-${barStack.index}-${bar.index}`}
                    x={bar.x}
                    y={bar.y}
                    width={bar.width}
                    height={bar.height}
                    fill={bar.color}
                  />
                ))
              )
            }
          </BarStackHorizontal>
          <AxisLeft
            hideTicks
            scale={monthScale}
            stroke={grey}
            tickLabelProps={() => ({
              fill: black,
              fontSize: 14,
              textAnchor: 'end',
              dy: '0.25em',
            })}
          />
          <AxisBottom
            hideTicks
            top={yMax}
            scale={percentageScale}
            stroke={grey}
            tickLabelProps={() => ({
              fill: black,
              fontSize: 14,
              textAnchor: 'middle',
            })}
          />
        </Group>
      </svg>

      <div
        style={{
          width: `${width}px`,
          display: 'flex',
          justifyContent: 'center',
          fontSize: '14px',
          marginTop: '-4rem',
        }}
      >
        <LegendOrdinal scale={colorScale} direction="row" labelMargin="0 15px 0 0" shapeHeight="8px" shapeWidth="8px" />
      </div>
    </div>
  );
}
