import React from 'react';
import { Input as NavInput, InputProps as NavInputProps } from 'nav-frontend-skjema';
import { UseFormRegisterReturn } from 'react-hook-form';

interface InputProps extends NavInputProps {
  register?: UseFormRegisterReturn;
}

function FormInput(props: InputProps) {
  const { register, ...others } = props;
  if (register) {
    const { ref, ...registerOthers } = register;
    return <NavInput inputRef={ref} {...registerOthers} {...others} />;
  }
  return <NavInput {...others} />;
}

export default FormInput;
