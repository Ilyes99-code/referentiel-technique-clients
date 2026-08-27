import React from "react";

interface ToastProps {
  show: boolean;
  message: string;
}

export const Toast: React.FC<ToastProps> = ({ show, message }) => (
  <div className={`toast-copied${show ? " show" : ""}`}>{message}</div>
);
