import React, { useEffect, useState } from 'react';
import { BarStackHorizontal } from '@visx/shape';
import { Group } from '@visx/group';
import { AxisBottom, AxisLeft } from '@visx/axis';
import { GridColumns } from '@visx/grid';
import { scaleBand, scaleLinear, scaleOrdinal } from '@visx/scale';
import { LegendOrdinal } from '@visx/legend';
import { Datapunkt } from './Datapunkt';
import useHentStatistikkFraFil from '../../../hooks/useHentStatistikkFraFil';

type Status = 'Arbeidstaker m. ytelse/oppf' | 'Kun arbeidstaker' | 'Registrert hos Nav' | 'Ukjent';

function isOfStatusType(value: any): value is Status {
  return ['Arbeidstaker m. ytelse/oppf', 'Kun arbeidstaker', 'Registrert hos Nav', 'Ukjent'].includes(value);
}

function csvObjectTilDatapunktArray(array: any[]): Datapunkt[] {
  return array.map(item => {
    return {
      tiltakstype: item['Tiltakstype'],
      'Arbeidstaker m. ytelse/oppf': item['Arbeidstaker m. ytelse/oppf'],
      'Kun arbeidstaker': item['Kun arbeidstaker'],
      'Registrert hos Nav': item['Registrert hos Nav'],
      Ukjent: item['Ukjent'],
      antallManeder: item['Antall Måneder'] + ' mnd',
    };
  });
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
/*
const data = [
  {
    tiltakstype: 'AFT',
    antallManeder: '12 mnd',
    'Arbeidstaker m. ytelse/oppf': 25,
    'Kun arbeidstaker': 30,
    'Registrert hos Nav': 30,
    Ukjent: 15,
  },
  {
    tiltakstype: 'AFT',
    antallManeder: '6 mnd',
    'Arbeidstaker m. ytelse/oppf': 30,
    'Kun arbeidstaker': 35,
    'Registrert hos Nav': 20,
    Ukjent: 15,
  },
  {
    tiltakstype: 'AFT',
    antallManeder: '3 mnd',
    'Arbeidstaker m. ytelse/oppf': 35,
    'Kun arbeidstaker': 25,
    'Registrert hos Nav': 15,
    Ukjent: 25,
  },
];
*/

// accessors
const getAntallManeder = (d: Datapunkt) => d.antallManeder;

export default function BarChart({ tiltakstype, width, height, margin = defaultMargin }: BarStackHorizontalProps) {
  const datatatat = useHentStatistikkFraFil();
  if (!datatatat || datatatat.length === 0) {
    return null;
  }
  const data = csvObjectTilDatapunktArray(datatatat).filter(item => item.tiltakstype === tiltakstype);

  if (!data || data.length === 0) {
    return null;
  }

  //prep data
  const keys = Object.keys(data[0]).filter(d => isOfStatusType(d)) as Status[];

  const percentageTotals = data.reduce((allTotals, currentMonth) => {
    const totalPercentage = keys.reduce((monthlyTotal, k) => {
      monthlyTotal += Number(currentMonth[k]);
      return monthlyTotal;
    }, 0);
    allTotals.push(totalPercentage);
    return allTotals;
  }, [] as number[]);

  // scales
  const percentageScale = scaleLinear<number>({
    domain: [0, Math.max(...percentageTotals)],
    nice: false,
  });
  const monthScale = scaleBand<string>({
    domain: data.map(getAntallManeder),
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
            data={data}
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
