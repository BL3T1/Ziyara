import { Card } from '../../../components/Card'
import type { ServiceEntity } from '../serviceModel'
import { getServicePrimaryImageUrl } from '../serviceModel'

interface ServiceCardProps {
  service: ServiceEntity
  onOpen: (service: ServiceEntity) => void
}

export function ServiceCard({ service, onOpen }: ServiceCardProps) {
  return (
    <Card className="p-0 overflow-hidden">
      <button type="button" onClick={() => onOpen(service)} className="block w-full text-left">
        <img
          src={getServicePrimaryImageUrl(service)}
          alt={service.name}
          className="h-44 w-full object-cover"
          loading="lazy"
        />
        <div className="p-5">
          <h3 className="font-semibold text-slate-900 dark:text-slate-100">{service.name}</h3>
          <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {service.rating != null ? `${service.rating.toFixed(1)} / 5` : 'No rating yet'}
          </div>
          <p className="mt-2 line-clamp-2 text-sm text-slate-600 dark:text-slate-300">
            {service.description || 'No description available.'}
          </p>
        </div>
      </button>
    </Card>
  )
}
