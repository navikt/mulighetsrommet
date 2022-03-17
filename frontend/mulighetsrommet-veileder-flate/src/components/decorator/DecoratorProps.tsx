export interface DecoratorProps {
  appname: string;
  fnr?: FnrContextvalue;
  enhet?: EnhetContextvalue;
  toggles?: TogglesConfig;
  markup?: Markup;
}

export interface TogglesConfig {
  visVeileder?: boolean;
}

export interface Markup {
  etterSokefelt?: string;
}

export interface ControlledContextvalue<T> extends BaseContextvalue<T> {
  value: string | null;
}

export interface UncontrolledContextvalue<T> extends BaseContextvalue<T> {
  initialValue: string | null;
}

export interface BaseContextvalue<T> {
  display: T;
  skipModal?: boolean;
  ignoreWsEvents?: boolean;

  onChange(value: string | null): void;
}

export type Contextvalue<T> = ControlledContextvalue<T> | UncontrolledContextvalue<T>;

export enum EnhetDisplay {
  // eslint-disable-next-line
  ENHET = 'ENHET',
  // eslint-disable-next-line
  ENHET_VALG = 'ENHET_VALG',
}

export enum FnrDisplay {
  // eslint-disable-next-line
  SOKEFELT = 'SOKEFELT',
}

export type EnhetContextvalue = Contextvalue<EnhetDisplay>;
export type FnrContextvalue = Contextvalue<FnrDisplay>;
