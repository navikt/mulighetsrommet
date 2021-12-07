import React, { useEffect } from 'react';
import './Toggle.less';
import classNames from 'classnames';
import { useDispatch } from 'react-redux';
import { ActionCreatorWithoutPayload } from '@reduxjs/toolkit';

export interface ToggleProps {
  toggle: boolean;
  onChange: ActionCreatorWithoutPayload;
  venstreTekst: string;
  hoyreTekst: string;
  className?: string;
}

const Toggle = ({ toggle, onChange, venstreTekst, hoyreTekst, className }: ToggleProps) => {
  const dispatch = useDispatch();

  useEffect(() => {
    const venstreSideTekst = document.querySelector<HTMLElement>(`.${venstreTekst}`)!;
    const hoyreSideTekst = document.querySelector<HTMLElement>(`.${hoyreTekst}`)!;

    if (toggle) {
      venstreSideTekst.style.color = 'white';
      venstreSideTekst.style.transitionDuration = '0.5s';
      hoyreSideTekst.style.color = 'black';
      hoyreSideTekst.style.transitionDuration = '0.5s';
    } else {
      venstreSideTekst.style.color = 'black';
      venstreSideTekst.style.transitionDuration = '0.5s';
      hoyreSideTekst.style.color = 'white';
      hoyreSideTekst.style.transitionDuration = '0.5s';
    }
  }, [toggle, hoyreTekst, venstreTekst]);

  return (
    <div className={classNames('switch-button', className)}>
      <input
        className="switch-button__checkbox"
        type="checkbox"
        onChange={() => dispatch(onChange())}
        id="switch-button"
        checked={!toggle}
      />
      <label className="switch-button-label" htmlFor="switch-button">
        <span className={classNames('switch-button__venstre-tekst', venstreTekst)}>{venstreTekst}</span>
      </label>
      <label>
        <span className={classNames('switch-button__hoyre-tekst', hoyreTekst)}>{hoyreTekst}</span>
      </label>
    </div>
  );
};

export default Toggle;
