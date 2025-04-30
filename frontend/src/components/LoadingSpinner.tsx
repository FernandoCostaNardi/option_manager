import React from 'react';
import { Loader2 } from 'lucide-react';

interface LoadingSpinnerProps {
  message: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ message }) => {
  return (
    <div className="flex justify-center items-center py-10">
      <Loader2 className="animate-spin w-8 h-8 text-purple-600" />
      <span className="ml-3 text-gray-600">{message}</span>
    </div>
  );
};