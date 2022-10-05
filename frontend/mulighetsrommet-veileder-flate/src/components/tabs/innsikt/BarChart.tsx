import { AxisBottom, AxisLeft } from '@visx/axis';
import { GridColumns } from '@visx/grid';
import { Group } from '@visx/group';
import { LegendOrdinal } from '@visx/legend';
import { scaleBand, scaleLinear, scaleOrdinal } from '@visx/scale';
import { BarStackHorizontal } from '@visx/shape';
import { SeriesPoint } from '@visx/shape/lib/types';
import { useTooltip, useTooltipInPortal, defaultStyles } from '@visx/tooltip';
import { StatistikkFraCsvFil } from '../../../core/api/models';
import useHentStatistikkFraFil from '../../../hooks/useHentStatistikkFraFil';
import styles from '../Detaljerfane.module.scss';
import barchartStyles from './Barchart.module.scss';
import { Datapunkt } from './Datapunkt';
import { localPoint } from '@visx/event';
import classNames from 'classnames';

const SISTE_AAR = 5;

type Status = 'Arbeidstaker m. ytelse/oppf' | 'Kun arbeidstaker' | 'Registrert hos Nav' | 'Ukjent';

function isOfStatusType(value: string): value is Status {
  return ['Arbeidstaker m. ytelse/oppf', 'Kun arbeidstaker', 'Registrert hos Nav', 'Ukjent'].includes(value);
}

function csvObjectArrayTilDatapunktArray(array: StatistikkFraCsvFil[]): Datapunkt[] {
  return array
    .sort((a, b) => parseInt(b['Antall Måneder']) - parseInt(a['Antall Måneder']))
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

function gjennomsnittForDatapunkterForSisteAar(
  datapunkter: Datapunkt[],
  antallAarTilbakeITid: number,
  tiltakstype: string
): Datapunkt[] {
  const punkter = datapunkter.reverse().slice(0, antallAarTilbakeITid);

  const punkterKronologisk = [...punkter.reverse()];
  const aar = [punkterKronologisk[0]?.År, punkterKronologisk[punkterKronologisk.length - 1]?.År]
    .filter(Boolean)
    .join(' - ');

  return punkter.reduce<Datapunkt[]>(
    (all, next, _, { length }) => {
      all[0]['Arbeidstaker m. ytelse/oppf'] += next['Arbeidstaker m. ytelse/oppf'] / length;
      all[0]['Kun arbeidstaker'] += next['Kun arbeidstaker'] / length;
      all[0]['Registrert hos Nav'] += next['Registrert hos Nav'] / length;
      all[0].Ukjent += next.Ukjent / length;
      all[0].tiltakstype = next.tiltakstype;
      all[0].antallManeder = next.antallManeder;
      return [...all];
    },
    [
      {
        'Arbeidstaker m. ytelse/oppf': 0,
        'Kun arbeidstaker': 0,
        'Registrert hos Nav': 0,
        Ukjent: 0,
        antallManeder: '',
        tiltakstype,
        År: aar,
      },
    ]
  );
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

const tooltipStyles = {
  ...defaultStyles,
  minWidth: 60,
  backgroundColor: 'rgba(0,0,0,0.9)',
  color: 'white',
};

// accessors
const getAntallManeder = (d: Datapunkt) => d.antallManeder;

type TooltipData = {
  bar: SeriesPoint<Datapunkt>;
  key: Status;
  index: number;
  height: number;
  width: number;
  x: number;
  y: number;
  color: string;
};

let tooltipTimeout: number;

export default function BarChart({ tiltakstype, width, height, margin = defaultMargin }: BarStackHorizontalProps) {
  const csvDataFraFil = useHentStatistikkFraFil();
  const { tooltipOpen, tooltipLeft, tooltipTop, tooltipData, hideTooltip, showTooltip } = useTooltip<TooltipData>();
  const { containerRef, TooltipInPortal } = useTooltipInPortal({
    // TooltipInPortal is rendered in a separate child of <body /> and positioned
    // with page coordinates which should be updated on scroll. consider using
    // Tooltip or TooltipWithBounds if you don't need to render inside a Portal
    scroll: true,
  });

  if (!csvDataFraFil || csvDataFraFil.length === 0) {
    return null;
  }
  const datapunkter = csvObjectArrayTilDatapunktArray(csvDataFraFil);

  if (!datapunkter || datapunkter.length === 0) {
    return null;
  }

  // prep data
  const datapunkterGrupperPerManed = datapunkter
    .filter(item => item.tiltakstype === tiltakstype)
    .reduce<{
      '3 mnd': Datapunkt[];
      '6 mnd': Datapunkt[];
      '12 mnd': Datapunkt[];
    }>(
      (all: any, next) => {
        all[next.antallManeder]?.push(next);
        return all;
      },
      { '3 mnd': [], '6 mnd': [], '12 mnd': [] }
    );

  const statsFor3Mnd = gjennomsnittForDatapunkterForSisteAar(
    datapunkterGrupperPerManed['3 mnd'],
    SISTE_AAR,
    tiltakstype
  );
  const statsFor6Mnd = gjennomsnittForDatapunkterForSisteAar(
    datapunkterGrupperPerManed['6 mnd'],
    SISTE_AAR,
    tiltakstype
  );
  const statsFor12Mnd = gjennomsnittForDatapunkterForSisteAar(
    datapunkterGrupperPerManed['12 mnd'],
    SISTE_AAR,
    tiltakstype
  );

  const dataForVisning: Datapunkt[] = [...statsFor3Mnd, ...statsFor6Mnd, ...statsFor12Mnd].reverse();

  const keys = Object.keys(dataForVisning[0]).filter(isOfStatusType);

  // scales
  const percentageScale = scaleLinear<number>({
    domain: [0, 100],
    nice: false,
  });

  const monthScale = scaleBand<string>({
    domain: dataForVisning.map(getAntallManeder),
    padding: 0.8,
  });
  const colorScale = scaleOrdinal<string, string>({
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
      <div style={{ width }} className={styles.tiltaksdetaljer_innsiktheader}>
        Status etter avgang siste {SISTE_AAR} år ({dataForVisning[0].År})
      </div>

      <svg ref={containerRef} width={width} height={height}>
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
            data={dataForVisning}
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
                    onMouseLeave={() => {
                      tooltipTimeout = window.setTimeout(() => {
                        hideTooltip();
                      }, 300);
                    }}
                    onMouseMove={event => {
                      if (tooltipTimeout) clearTimeout(tooltipTimeout);
                      // TooltipInPortal expects coordinates to be relative to containerRef
                      // localPoint returns coordinates relative to the nearest SVG, which
                      // is what containerRef is set to in this example.
                      const eventSvgCoords = localPoint(event);
                      const left = bar.x + bar.width / 2;
                      showTooltip({
                        tooltipData: bar,
                        tooltipTop: eventSvgCoords?.y,
                        tooltipLeft: left,
                      });
                    }}
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
      {tooltipOpen && tooltipData && (
        <TooltipInPortal top={tooltipTop} left={tooltipLeft} style={tooltipStyles}>
          <div className={classNames(barchartStyles.tooltip_container, barchartStyles.tooltip_container_row)}>
            <div>
              <span className={barchartStyles.tooltip_color_icon} style={{ background: colorScale(tooltipData.key) }} />
            </div>
            <div className={barchartStyles.tooltip_container_column}>
              <span className={classNames(barchartStyles.tooltip_data_text, barchartStyles.tooltip_data_number)}>
                {tooltipData.bar.data[tooltipData.key].toFixed(2)}%
              </span>
              <span className={classNames(barchartStyles.tooltip_data_text, barchartStyles.tooltip_data_label)}>
                {tooltipData.key}
              </span>
            </div>
          </div>
        </TooltipInPortal>
      )}
    </div>
  );
}
