import React from 'react';
import { AlertCircle } from 'lucide-react';

interface ErrorMessageProps {
  message: string;
}

export const ErrorMessage: React.FC<ErrorMessageProps> = ({ message }) => {
  return (
    <div className="bg-red-50 p-4 rounded-lg text-red-700 flex items-start">
      <AlertCircle className="w-5 h-5 mr-2 mt-0.5 flex-shrink-0" />
      <p>{message}</p>
    </div>
  );
};